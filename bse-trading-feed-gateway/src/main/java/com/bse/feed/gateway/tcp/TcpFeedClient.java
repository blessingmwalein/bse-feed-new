package com.bse.feed.gateway.tcp;

import com.bse.feed.core.event.MarketDataEvent;
import com.bse.feed.core.event.MarketDataEventBus;
import com.bse.feed.core.model.FeedStatus;
import com.bse.feed.gateway.decoder.FastMessageDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * TCP client for BSE Snapshot and Replay channels.
 * Uses FIX-like session layer with FAST transfer encoding.
 *
 * BSE specifics:
 * - Client messages: FAST transfer-encoded but NOT field-encoded
 * - Server app messages: FAST transfer AND field-encoded
 * - Server admin messages: FAST transfer-encoded only (no field encoding)
 *
 * Snapshot Channel: Request current state for specific instruments
 * Replay Channel: Request historical messages by sequence range
 *
 * Connection parameters:
 *   Test Snapshot/Replay: 192.168.90.18:540
 *   Prod Snapshot: 192.168.4.22:5521 / 192.168.4.23:5521
 *   Prod Replay:   192.168.4.22:5520 / 192.168.4.23:5520
 */
public class TcpFeedClient {

    private static final Logger log = LoggerFactory.getLogger(TcpFeedClient.class);
    private static final DateTimeFormatter SENDING_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

    public enum ChannelType {
        SNAPSHOT,
        REPLAY
    }

    private final ChannelType channelType;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String applId;       // Server ApplID (usually "1")
    private final int heartbeatInterval;

    private final FastMessageDecoder decoder;
    private final MarketDataEventBus eventBus;
    private final FeedStatus feedStatus;

    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private volatile boolean connected = false;
    private volatile boolean running = false;

    // Session state
    private int msgSeqNum = 1;

    public TcpFeedClient(ChannelType channelType, String host, int port,
                          String username, String password, String applId,
                          int heartbeatInterval,
                          FastMessageDecoder decoder, MarketDataEventBus eventBus) {
        this.channelType = channelType;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.applId = applId;
        this.heartbeatInterval = heartbeatInterval;
        this.decoder = decoder;
        this.eventBus = eventBus;
        this.feedStatus = new FeedStatus("TCP-" + channelType.name());
    }

    /**
     * Connect to the BSE TCP channel and perform FAST-encoded FIX Logon.
     */
    public void connect() throws IOException {
        log.info("Connecting to {} at {}:{}", channelType, host, port);

        socket = new Socket(host, port);
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(heartbeatInterval * 3000);  // 3x heartbeat as timeout

        outputStream = new BufferedOutputStream(socket.getOutputStream());
        inputStream = new BufferedInputStream(socket.getInputStream());

        connected = true;
        feedStatus.setState(FeedStatus.ConnectionState.CONNECTING);
        feedStatus.setConnectionTime(Instant.now());

        // Send Logon using FAST transfer encoding
        sendLogon();

        // Read Logon response
        byte[] response = readFastMessage();
        if (response != null) {
            MarketDataEvent event = decoder.decode(response, "TCP");
            if (event != null) {
                log.info("{} Logon successful", channelType);
                feedStatus.setState(FeedStatus.ConnectionState.CONNECTED);
            }
        }

        running = true;
        log.info("Connected to {} channel successfully", channelType);
    }

    /**
     * Send a FAST-encoded Logon message.
     * Per BSE spec: Client messages are FAST transfer-encoded but NOT field-encoded.
     * This means the FAST PMAP and template ID are present but field values are raw FIX tags.
     */
    private void sendLogon() throws IOException {
        // Build FIX Logon fields
        StringBuilder fixBody = new StringBuilder();
        fixBody.append("35=A\u0001");                          // MsgType = Logon
        fixBody.append("52=").append(getCurrentSendingTime()).append("\u0001");  // SendingTime
        fixBody.append("1180=").append(applId).append("\u0001");  // AppID
        fixBody.append("108=").append(heartbeatInterval).append("\u0001");  // HeartBtInt
        fixBody.append("553=").append(username).append("\u0001");  // Username
        fixBody.append("554=").append(password).append("\u0001");  // Password

        // For FAST transfer encoding, we need to wrap in FAST framing
        // The BSE uses a simple length-prefixed FAST message over TCP
        byte[] body = fixBody.toString().getBytes(StandardCharsets.US_ASCII);

        // FAST over TCP uses a 4-byte big-endian length prefix
        ByteBuffer header = ByteBuffer.allocate(4);
        header.putInt(body.length);
        header.flip();

        outputStream.write(header.array());
        outputStream.write(body);
        outputStream.flush();

        msgSeqNum++;
        log.debug("Sent Logon to {} (body_len={})", channelType, body.length);
    }

    /**
     * Send a Market Data Request for specific symbols (Snapshot channel).
     *
     * @param mdReqId Unique request ID
     * @param symbols List of symbols to request snapshots for
     */
    public void sendMarketDataRequest(String mdReqId, String... symbols) throws IOException {
        if (!connected) throw new IOException("Not connected");

        StringBuilder fixBody = new StringBuilder();
        fixBody.append("35=V\u0001");
        fixBody.append("52=").append(getCurrentSendingTime()).append("\u0001");
        fixBody.append("262=").append(mdReqId).append("\u0001");
        fixBody.append("263=0\u0001");  // SubscriptionRequestType=0 (Snapshot)

        // MDEntryTypes
        fixBody.append("267=2\u0001");  // Number of entry types
        fixBody.append("269=0\u0001");  // Bid
        fixBody.append("269=1\u0001");  // Offer

        // Symbols
        fixBody.append("146=").append(symbols.length).append("\u0001");
        for (String symbol : symbols) {
            fixBody.append("55=").append(symbol).append("\u0001");
        }

        byte[] body = fixBody.toString().getBytes(StandardCharsets.US_ASCII);
        ByteBuffer header = ByteBuffer.allocate(4);
        header.putInt(body.length);
        header.flip();

        outputStream.write(header.array());
        outputStream.write(body);
        outputStream.flush();

        msgSeqNum++;
        log.info("Sent MarketDataRequest for {} symbols, reqId={}", symbols.length, mdReqId);
    }

    /**
     * Send an Application Message Request for replay (Replay channel).
     *
     * @param applReqId Unique request ID
     * @param refApplId Application ID to replay
     * @param beginSeq  Start sequence number
     * @param endSeq    End sequence number
     */
    public void sendReplayRequest(String applReqId, String refApplId,
                                   long beginSeq, long endSeq) throws IOException {
        if (!connected) throw new IOException("Not connected");

        StringBuilder fixBody = new StringBuilder();
        fixBody.append("35=BW\u0001");  // ApplicationMessageRequest
        fixBody.append("52=").append(getCurrentSendingTime()).append("\u0001");
        fixBody.append("1346=").append(applReqId).append("\u0001");  // ApplReqID
        fixBody.append("1347=0\u0001");  // ApplReqType=0 (Retransmission)

        // NoApplIDs
        fixBody.append("1351=1\u0001");
        fixBody.append("1355=").append(refApplId).append("\u0001");  // RefApplID
        fixBody.append("1182=").append(beginSeq).append("\u0001");   // ApplBegSeqNum
        fixBody.append("1183=").append(endSeq).append("\u0001");     // ApplEndSeqNum

        byte[] body = fixBody.toString().getBytes(StandardCharsets.US_ASCII);
        ByteBuffer header = ByteBuffer.allocate(4);
        header.putInt(body.length);
        header.flip();

        outputStream.write(header.array());
        outputStream.write(body);
        outputStream.flush();

        msgSeqNum++;
        log.info("Sent ReplayRequest: applId={}, range=[{}-{}]", refApplId, beginSeq, endSeq);
    }

    /**
     * Send a Heartbeat message.
     */
    public void sendHeartbeat() throws IOException {
        if (!connected) return;

        StringBuilder fixBody = new StringBuilder();
        fixBody.append("35=0\u0001");
        fixBody.append("52=").append(getCurrentSendingTime()).append("\u0001");
        fixBody.append("1180=").append(applId).append("\u0001");
        fixBody.append("1399=0\u0001");  // AppNewSeqNum

        byte[] body = fixBody.toString().getBytes(StandardCharsets.US_ASCII);
        ByteBuffer header = ByteBuffer.allocate(4);
        header.putInt(body.length);
        header.flip();

        outputStream.write(header.array());
        outputStream.write(body);
        outputStream.flush();

        log.trace("Sent heartbeat to {}", channelType);
    }

    /**
     * Read and process incoming messages until disconnected.
     * Should be called in a loop from a dedicated thread.
     */
    public MarketDataEvent readNextMessage() throws IOException {
        byte[] data = readFastMessage();
        if (data == null) return null;

        feedStatus.setLastMessageTime(Instant.now());
        feedStatus.incrementMessagesReceived();

        MarketDataEvent event = decoder.decode(data, "TCP");
        if (event != null) {
            feedStatus.setLastSequenceNumber(event.getApplSeqNum());
        }
        return event;
    }

    /**
     * Read a single FAST message from the TCP stream.
     * Messages are framed with a 4-byte big-endian length prefix.
     */
    private byte[] readFastMessage() throws IOException {
        // Read 4-byte length
        byte[] lenBytes = new byte[4];
        int read = readFully(lenBytes, 4);
        if (read < 4) return null;

        int length = ByteBuffer.wrap(lenBytes).getInt();
        if (length <= 0 || length > 65536) {
            log.warn("Invalid message length: {}", length);
            return null;
        }

        // Read message body
        byte[] body = new byte[length];
        read = readFully(body, length);
        if (read < length) {
            log.warn("Incomplete message: expected {} bytes, got {}", length, read);
            return null;
        }

        return body;
    }

    private int readFully(byte[] buffer, int length) throws IOException {
        int totalRead = 0;
        while (totalRead < length) {
            int read = inputStream.read(buffer, totalRead, length - totalRead);
            if (read < 0) return totalRead;
            totalRead += read;
        }
        return totalRead;
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        running = false;
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.warn("Error closing socket: {}", e.getMessage());
        }
        feedStatus.setState(FeedStatus.ConnectionState.DISCONNECTED);
        log.info("Disconnected from {} at {}:{}", channelType, host, port);
    }

    private String getCurrentSendingTime() {
        return SENDING_TIME_FMT.format(Instant.now());
    }

    public boolean isConnected() { return connected; }
    public boolean isRunning() { return running; }
    public FeedStatus getFeedStatus() { return feedStatus; }
    public ChannelType getChannelType() { return channelType; }
}
