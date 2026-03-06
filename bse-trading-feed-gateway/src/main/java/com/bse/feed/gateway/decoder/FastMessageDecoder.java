package com.bse.feed.gateway.decoder;

import com.bse.feed.core.enums.*;
import com.bse.feed.core.event.MarketDataEvent;
import com.bse.feed.core.model.HeartbeatMessage;
import com.bse.feed.core.model.MarketDataEntry;
import com.bse.feed.core.model.SecurityDefinition;

import org.openfast.*;
import org.openfast.codec.FastDecoder;
import org.openfast.template.MessageTemplate;
import org.openfast.template.TemplateRegistry;
import org.openfast.template.loader.XMLMessageTemplateLoader;
import org.openfast.GroupValue;
import org.openfast.SequenceValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Decodes BSE FAST-UDP messages using the OpenFAST library.
 * Loads FAST 1.1 templates from FastGWMsgConfig.xml and converts
 * OpenFAST Message objects into domain model objects.
 *
 * Template IDs:
 *   0  - Heartbeat (admin, transfer-encoded only)
 *   3  - Heartbeat (app)
 *   14 - SecurityDefinition
 *   15 - SecurityStatus
 *   18 - MarketDataSnapshot (Full Refresh)
 *   20 - MarketDataIncrementalRefresh
 *   21 - QuoteRequest
 *   24 - News
 *   25 - MarketDataRequestReject
 *   26 - BusinessMessageReject
 *   27 - ApplicationMessageRequestAck
 *   28 - ApplicationMessageReport
 */
public class FastMessageDecoder {

    private static final Logger log = LoggerFactory.getLogger(FastMessageDecoder.class);

    private TemplateRegistry templateRegistry;
    private Context decoderContext;

    // Template IDs from FastGWMsgConfig.xml
    public static final int TEMPLATE_LOGON = 1;
    public static final int TEMPLATE_LOGOUT = 2;
    public static final int TEMPLATE_HEARTBEAT = 3;
    public static final int TEMPLATE_SEC_DEF_REQUEST = 4;
    public static final int TEMPLATE_SECURITY_DEFINITION = 14;
    public static final int TEMPLATE_SECURITY_STATUS = 15;
    public static final int TEMPLATE_SNAPSHOT_MD_ENTRIES = 17;
    public static final int TEMPLATE_MARKET_DATA_SNAPSHOT = 18;
    public static final int TEMPLATE_INC_REFRESH_MD_ENTRIES = 19;
    public static final int TEMPLATE_INCREMENTAL_REFRESH = 20;
    public static final int TEMPLATE_QUOTE_REQUEST = 21;
    public static final int TEMPLATE_NEWS = 24;
    public static final int TEMPLATE_MD_REQUEST_REJECT = 25;
    public static final int TEMPLATE_BUSINESS_MESSAGE_REJECT = 26;
    public static final int TEMPLATE_APP_MSG_REQUEST_ACK = 27;
    public static final int TEMPLATE_APP_MSG_REPORT = 28;

    /**
     * Initialize the decoder by loading FAST templates from the given resource path.
     *
     * @param templateResourcePath classpath resource path (e.g., "/templates/FastGWMsgConfig.xml")
     */
    public void initialize(String templateResourcePath) {
        log.info("Loading FAST templates from: {}", templateResourcePath);

        InputStream templateStream = getClass().getResourceAsStream(templateResourcePath);
        if (templateStream == null) {
            throw new IllegalStateException("FAST template file not found: " + templateResourcePath);
        }

        XMLMessageTemplateLoader loader = new XMLMessageTemplateLoader();
        loader.setLoadTemplateIdFromAuxId(false);
        MessageTemplate[] templates = loader.load(templateStream);

        templateRegistry = loader.getTemplateRegistry();
        for (MessageTemplate template : templates) {
            log.debug("Registered FAST template: id={}, name={}", template.getId(), template.getName());
        }

        decoderContext = new Context();
        decoderContext.setTemplateRegistry(templateRegistry);

        log.info("Loaded {} FAST templates", templates.length);
    }

    /**
     * Initialize with a direct InputStream (for testing or custom loading).
     */
    public void initialize(InputStream templateStream) {
        XMLMessageTemplateLoader loader = new XMLMessageTemplateLoader();
        loader.setLoadTemplateIdFromAuxId(false);
        MessageTemplate[] templates = loader.load(templateStream);

        templateRegistry = loader.getTemplateRegistry();

        decoderContext = new Context();
        decoderContext.setTemplateRegistry(templateRegistry);

        log.info("Loaded {} FAST templates from stream", templates.length);
    }

    /**
     * Decode a single FAST message from raw bytes.
     *
     * @param data   Raw FAST-encoded bytes
     * @param offset Start offset in the array
     * @param length Number of bytes to decode
     * @param feedSource "A" or "B" to identify the source feed
     * @return Decoded MarketDataEvent, or null if decoding fails
     */
    public MarketDataEvent decode(byte[] data, int offset, int length, String feedSource) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data, offset, length);
            FastDecoder decoder = new FastDecoder(decoderContext, bais);

            Message msg = decoder.readMessage();
            if (msg == null) {
                log.warn("Failed to decode FAST message: null result");
                return null;
            }

            int templateId = parseTemplateId(msg.getTemplate());
            return mapMessage(msg, templateId, feedSource);

        } catch (Exception e) {
            log.error("FAST decode error: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Decode a FAST message from the full byte array.
     */
    public MarketDataEvent decode(byte[] data, String feedSource) {
        return decode(data, 0, data.length, feedSource);
    }

    /**
     * Decode all FAST messages from a multi-message packet.
     *
     * @param data       Raw packet bytes
     * @param feedSource "A" or "B"
     * @return List of decoded events
     */
    public List<MarketDataEvent> decodeAll(byte[] data, String feedSource) {
        List<MarketDataEvent> events = new ArrayList<>();
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            FastDecoder decoder = new FastDecoder(decoderContext, bais);

            while (bais.available() > 0) {
                try {
                    Message msg = decoder.readMessage();
                    if (msg == null) break;

                    int templateId = parseTemplateId(msg.getTemplate());
                    MarketDataEvent event = mapMessage(msg, templateId, feedSource);
                    if (event != null) {
                        events.add(event);
                    }
                } catch (Exception e) {
                    log.warn("Error decoding message in multi-message packet: {}", e.getMessage());
                    break;
                }
            }
        } catch (Exception e) {
            log.error("FAST decode error in packet: {}", e.getMessage(), e);
        }
        return events;
    }

    /**
     * Map an OpenFAST Message to a domain MarketDataEvent based on template ID.
     */
    private MarketDataEvent mapMessage(Message msg, int templateId, String feedSource) {
        return switch (templateId) {
            case TEMPLATE_HEARTBEAT -> mapHeartbeat(msg, feedSource);
            case TEMPLATE_MARKET_DATA_SNAPSHOT -> mapSnapshot(msg, feedSource);
            case TEMPLATE_INCREMENTAL_REFRESH -> mapIncrementalRefresh(msg, feedSource);
            case TEMPLATE_SECURITY_DEFINITION -> mapSecurityDefinition(msg, feedSource);
            case TEMPLATE_SECURITY_STATUS -> mapSecurityStatus(msg, feedSource);
            case TEMPLATE_NEWS -> mapNews(msg, feedSource);
            case TEMPLATE_QUOTE_REQUEST -> mapQuoteRequest(msg, feedSource);
            case TEMPLATE_MD_REQUEST_REJECT -> mapRequestReject(msg, feedSource);
            case TEMPLATE_LOGON, TEMPLATE_LOGOUT -> {
                log.debug("Received session message: template={}", templateId);
                yield null;
            }
            default -> {
                log.debug("Unhandled template ID: {}", templateId);
                yield null;
            }
        };
    }

    // ==================== HEARTBEAT ====================

    private MarketDataEvent mapHeartbeat(Message msg, String feedSource) {
        String applId = getStringField(msg, "AppID");
        long applSeqNum = getLongField(msg, "AppNewSeqNum");

        HeartbeatMessage hb = new HeartbeatMessage(applId, applSeqNum);
        log.trace("Heartbeat: applId={}, seq={}", applId, applSeqNum);

        MarketDataEntry entry = new MarketDataEntry();
        entry.setApplId(applId);
        entry.setApplSeqNum(applSeqNum);
        entry.setTemplateId(TEMPLATE_HEARTBEAT);

        return new MarketDataEvent(
                MarketDataEvent.EventType.HEARTBEAT,
                Collections.singletonList(entry),
                applSeqNum, applId, TEMPLATE_HEARTBEAT, feedSource
        );
    }

    // ==================== SNAPSHOT (W) ====================

    private MarketDataEvent mapSnapshot(Message msg, String feedSource) {
        String applId = getStringField(msg, "AppID");
        long applSeqNum = getLongField(msg, "ApplSeqNum");
        String symbol = getStringField(msg, "Symbol");
        long rptSeq = getLongField(msg, "RptSeq");
        int mdBookType = getIntField(msg, "MDBookType", 3);
        int mdSubBookType = getIntField(msg, "MDSubBookType", 1);
        String lastFragment = getStringField(msg, "LastFragment");
        String mdReqId = getStringField(msg, "MDReqID");
        String lastRptRequested = getStringField(msg, "LastRptRequested");
        Integer mdSecurityTradingStatus = getOptionalInt(msg, "MDSecurityTradingStatus");
        Integer mdHaltReason = getOptionalInt(msg, "MDHaltReason");
        Long lastMsgSeqNumProcessed = getOptionalLong(msg, "LastMsgSeqNumProcessed");

        List<MarketDataEntry> entries = new ArrayList<>();

        // Parse MD Entries
        SequenceValue mdEntries = msg.getSequence("SnapshotMDEntries");
        if (mdEntries != null) {
            for (int i = 0; i < mdEntries.getLength(); i++) {
                GroupValue group = mdEntries.get(i);
                MarketDataEntry entry = new MarketDataEntry();

                // Header-level fields
                entry.setApplId(applId);
                entry.setApplSeqNum(applSeqNum);
                entry.setMsgType("W");
                entry.setTemplateId(TEMPLATE_MARKET_DATA_SNAPSHOT);
                entry.setSymbol(symbol);
                entry.setRptSeq(rptSeq);
                entry.setMdBookType(mdBookType);
                entry.setMdSubBookTypeRaw(mdSubBookType);
                entry.setSubBookType(MDSubBookType.fromCode(mdSubBookType));
                entry.setLastFragment(lastFragment);
                entry.setMdReqId(mdReqId);
                entry.setLastRptRequested(lastRptRequested);
                entry.setMdSecurityTradingStatus(mdSecurityTradingStatus);
                entry.setMdHaltReason(mdHaltReason);
                entry.setLastMsgSeqNumProcessed(lastMsgSeqNumProcessed);

                // Entry-level fields
                String entryTypeRaw = getStringField(group, "MDEntryType");
                entry.setMdEntryTypeRaw(entryTypeRaw);
                entry.setEntryType(MDEntryType.fromCode(entryTypeRaw));

                entry.setPrice(getDecimalField(group, "MDEntryPx"));
                entry.setYield(getDecimalField(group, "Yield"));
                entry.setLastParPx(getDecimalField(group, "LastParPx"));
                entry.setSize(getDecimalField(group, "MDEntrySize"));
                entry.setMdEntryId(getStringField(group, "MDEntryID"));
                entry.setMdEntryDate(getStringField(group, "MDEntryDate"));
                entry.setMdEntryTime(getStringField(group, "MDEntryTime"));
                entry.setNumberOfOrders(getOptionalInt(group, "NumberOfOrders"));
                entry.setMdEntryPositionNo(getOptionalInt(group, "MDEntryPositionNo"));
                entry.setMdPriceLevel(getOptionalInt(group, "MDPriceLevel"));
                entry.setOpenCloseIndicator(getOptionalInt(group, "OpenCloseIndicator"));
                entry.setAonStatus(getOptionalInt(group, "AONStatus"));
                entry.setAonSide(getOptionalInt(group, "AONSide"));

                // Stipulations
                String settleType = getStringField(group, "SettleType");
                if (settleType != null) {
                    try {
                        entry.setSettleType(Integer.parseInt(settleType));
                    } catch (NumberFormatException ignored) {}
                }

                // Party IDs
                SequenceValue partyIds = getSequenceField(group, "NoPartyIDs");
                if (partyIds != null && partyIds.getLength() > 0) {
                    GroupValue party = partyIds.get(0);
                    entry.setPartyId(getStringField(party, "PartyID"));
                    entry.setPartyIdSource(getStringField(party, "PartyIDSource"));
                    entry.setPartyRole(getOptionalInt(party, "PartyRole"));
                }

                entries.add(entry);
            }
        }

        log.debug("Snapshot: {} entries for {} seq={}", entries.size(), symbol, applSeqNum);
        return new MarketDataEvent(
                MarketDataEvent.EventType.SNAPSHOT,
                entries, applSeqNum, applId, TEMPLATE_MARKET_DATA_SNAPSHOT, feedSource
        );
    }

    // ==================== INCREMENTAL REFRESH (X) ====================

    private MarketDataEvent mapIncrementalRefresh(Message msg, String feedSource) {
        String applId = getStringField(msg, "AppID");
        long applSeqNum = getLongField(msg, "ApplSeqNum");
        int mdBookType = getIntField(msg, "MDBookType", 3);
        String mdReqId = getStringField(msg, "MDReqID");
        String lastRptRequested = getStringField(msg, "LastRptRequested");
        Integer recoveryTrdIndicator = getOptionalInt(msg, "RecoveryTrdIndicator");
        String lastFragment = getStringField(msg, "LastFragment");

        List<MarketDataEntry> entries = new ArrayList<>();

        SequenceValue mdEntries = msg.getSequence("NoMDEntries");
        if (mdEntries != null) {
            for (int i = 0; i < mdEntries.getLength(); i++) {
                GroupValue group = mdEntries.get(i);
                MarketDataEntry entry = new MarketDataEntry();

                // Header-level
                entry.setApplId(applId);
                entry.setApplSeqNum(applSeqNum);
                entry.setMsgType("X");
                entry.setTemplateId(TEMPLATE_INCREMENTAL_REFRESH);
                entry.setMdBookType(mdBookType);
                entry.setMdReqId(mdReqId);
                entry.setLastRptRequested(lastRptRequested);
                entry.setRecoveryTrdIndicator(recoveryTrdIndicator);
                entry.setLastFragment(lastFragment);

                // Update action
                int actionCode = getIntField(group, "MDUpdateAction", 0);
                entry.setUpdateAction(MDUpdateAction.fromCode(actionCode));

                // Sub-book type
                int subBookCode = getIntField(group, "MDSubBookType", 1);
                entry.setMdSubBookTypeRaw(subBookCode);
                entry.setSubBookType(MDSubBookType.fromCode(subBookCode));

                // Entry type
                String entryTypeRaw = getStringField(group, "MDEntryType");
                entry.setMdEntryTypeRaw(entryTypeRaw);
                entry.setEntryType(MDEntryType.fromCode(entryTypeRaw));

                // Instrument
                entry.setSymbol(getStringField(group, "Symbol"));
                Integer rptSeq = getOptionalInt(group, "RptSeq");
                if (rptSeq != null) entry.setRptSeq(rptSeq);

                // Price/Size
                entry.setPrice(getDecimalField(group, "MDEntryPx"));
                entry.setYield(getDecimalField(group, "Yield"));
                entry.setLastParPx(getDecimalField(group, "LastParPx"));
                entry.setSize(getDecimalField(group, "MDEntrySize"));
                entry.setMdEntryId(getStringField(group, "MDEntryID"));
                entry.setMdEntryDate(getStringField(group, "MDEntryDate"));
                entry.setMdEntryTime(getStringField(group, "MDEntryTime"));

                // Counts & levels
                entry.setNumberOfOrders(getOptionalInt(group, "NumberOfOrders"));
                entry.setMdEntryPositionNo(getOptionalInt(group, "MDEntryPositionNo"));
                entry.setMdPriceLevel(getOptionalInt(group, "MDPriceLevel"));
                entry.setMdQuoteType(getOptionalInt(group, "MDQuoteType"));

                // Trade fields
                entry.setTradeCondition(getStringField(group, "TradeCondition"));
                entry.setMatchType(getStringField(group, "MatchType"));
                entry.setTrdSubType(getOptionalInt(group, "TrdSubType"));
                entry.setTrdType(getOptionalInt(group, "TrdType"));
                entry.setSecondaryTradeId(getStringField(group, "SecondaryTradeID"));

                // Open/Close
                entry.setOpenCloseIndicator(getOptionalInt(group, "OpenCloseIndicator"));

                // MiFID II
                entry.setSecurityAltId(getStringField(group, "SecurityAltID"));
                entry.setSecurityAltIdSource(getStringField(group, "SecurityAltIDSource"));
                entry.setCurrency(getStringField(group, "Currency"));
                entry.setPriceNotation(getOptionalInt(group, "PriceNotation"));
                entry.setMdMkt(getStringField(group, "MDMkt"));
                entry.setUnderlyingNotional(getDecimalField(group, "UnderlyingNotional"));
                entry.setUnderlyingNotionalCurrency(getStringField(group, "UnderlyingNotionalCurrency"));
                entry.setTzTransactTime(getStringField(group, "TZTransactTime"));
                entry.setTzPublicationTime(getStringField(group, "TZPublicationTime"));

                // Post-trade transparency
                entry.setPtBenchmarkTransactionFlag(getStringField(group, "PTBenchmarkTransactionFlag"));
                entry.setPtPriceFormingTradesFlag(getStringField(group, "PTPriceFormingTradesFlag"));
                entry.setPtCancellationFlag(getStringField(group, "PTCancellationFlag"));
                entry.setPtAmendmentFlag(getStringField(group, "PTAmendmentFlag"));

                // Statistics & AON
                entry.setMdStatType(getOptionalInt(group, "MDStatType"));
                entry.setAonStatus(getOptionalInt(group, "AONStatus"));
                entry.setAonSide(getOptionalInt(group, "AONSide"));

                // Settle
                String settleType = getStringField(group, "SettleType");
                if (settleType != null) {
                    try {
                        entry.setSettleType(Integer.parseInt(settleType));
                    } catch (NumberFormatException ignored) {}
                }

                // Price band
                entry.setPriceLimitEvent(getOptionalInt(group, "PBPriceLimitEvent"));
                entry.setSecurityTradingStatus(getOptionalInt(group, "SecurityTradingStatus"));
                entry.setPbAffectedSide(getOptionalInt(group, "PBAffectedSide"));
                entry.setPriceBandLevel(getOptionalInt(group, "PriceBandLevel"));
                entry.setImpliedTradeFlag(getOptionalInt(group, "ImpliedTradeFlag"));

                // Party IDs
                SequenceValue partyIds = getSequenceField(group, "NoPartyIDs");
                if (partyIds != null && partyIds.getLength() > 0) {
                    GroupValue party = partyIds.get(0);
                    entry.setPartyId(getStringField(party, "PartyID"));
                    entry.setPartyIdSource(getStringField(party, "PartyIDSource"));
                    entry.setPartyRole(getOptionalInt(party, "PartyRole"));
                }

                entries.add(entry);
            }
        }

        log.debug("IncrRefresh: {} entries, seq={}", entries.size(), applSeqNum);
        return new MarketDataEvent(
                MarketDataEvent.EventType.INCREMENTAL_REFRESH,
                entries, applSeqNum, applId, TEMPLATE_INCREMENTAL_REFRESH, feedSource
        );
    }

    // ==================== SECURITY DEFINITION (d) ====================

    private MarketDataEvent mapSecurityDefinition(Message msg, String feedSource) {
        String applId = getStringField(msg, "ApplID");
        long applSeqNum = getLongField(msg, "ApplSeqNum");

        MarketDataEntry entry = new MarketDataEntry();
        entry.setApplId(applId);
        entry.setApplSeqNum(applSeqNum);
        entry.setMsgType("d");
        entry.setTemplateId(TEMPLATE_SECURITY_DEFINITION);
        entry.setSymbol(getStringField(msg, "Symbol"));

        log.debug("SecurityDefinition: {} seq={}", entry.getSymbol(), applSeqNum);
        return new MarketDataEvent(
                MarketDataEvent.EventType.SECURITY_DEFINITION,
                Collections.singletonList(entry),
                applSeqNum, applId, TEMPLATE_SECURITY_DEFINITION, feedSource
        );
    }

    /**
     * Extract a full SecurityDefinition model from a FAST message.
     * Should be called when template ID = 14.
     */
    public SecurityDefinition extractSecurityDefinition(Message msg) {
        SecurityDefinition def = new SecurityDefinition();
        def.setSymbol(getStringField(msg, "Symbol"));
        def.setSecurityStatus(getStringField(msg, "SecurityStatus"));
        def.setCfiCode(getStringField(msg, "CFICode"));
        def.setSecurityType(getStringField(msg, "SecurityType"));
        def.setSecuritySubType(getStringField(msg, "SecuritySubType"));
        def.setMaturityDate(getStringField(msg, "MaturityDate"));
        def.setCorporateAction(getStringField(msg, "CorporateAction"));
        def.setIssuer(getStringField(msg, "Issuer"));
        def.setIssueDate(getStringField(msg, "IssueDate"));
        def.setStrikePrice(getDecimalField(msg, "StrikePrice"));
        def.setCouponRate(getDecimalField(msg, "CouponRate"));
        def.setSecurityGroup(getStringField(msg, "SecurityGroup"));

        Integer putOrCall = getOptionalInt(msg, "PutOrCall");
        if (putOrCall != null) def.setPutOrCall(putOrCall);

        Integer priceType = getOptionalInt(msg, "PriceType");
        if (priceType != null) def.setPriceType(priceType);

        Integer listMethod = getOptionalInt(msg, "ListMethod");
        if (listMethod != null) def.setListMethod(listMethod);

        // Security Alt IDs (e.g. ISIN)
        SequenceValue altIds = getSequenceField(msg, "NoSecurityAltID");
        if (altIds != null) {
            for (int i = 0; i < altIds.getLength(); i++) {
                GroupValue g = altIds.get(i);
                String altId = getStringField(g, "SecurityAltID");
                String altIdSrc = getStringField(g, "SecurityAltIDSource");
                if ("4".equals(altIdSrc)) {
                    def.setIsin(altId);
                }
            }
        }

        return def;
    }

    // ==================== SECURITY STATUS (f) ====================

    private MarketDataEvent mapSecurityStatus(Message msg, String feedSource) {
        String applId = getStringField(msg, "ApplID");
        long applSeqNum = getLongField(msg, "ApplSeqNum");
        String symbol = getStringField(msg, "Symbol");

        MarketDataEntry entry = new MarketDataEntry();
        entry.setApplId(applId);
        entry.setApplSeqNum(applSeqNum);
        entry.setMsgType("f");
        entry.setTemplateId(TEMPLATE_SECURITY_STATUS);
        entry.setSymbol(symbol);

        Integer tradingStatus = getOptionalInt(msg, "SecurityTradingStatus");
        entry.setSecurityTradingStatus(tradingStatus);

        Integer haltReason = getOptionalInt(msg, "HaltReason");
        entry.setMdHaltReason(haltReason);

        int subBookType = getIntField(msg, "MDSubBookType", 1);
        entry.setMdSubBookTypeRaw(subBookType);
        entry.setSubBookType(MDSubBookType.fromCode(subBookType));

        log.debug("SecurityStatus: {} status={} seq={}", symbol, tradingStatus, applSeqNum);
        return new MarketDataEvent(
                MarketDataEvent.EventType.SECURITY_STATUS,
                Collections.singletonList(entry),
                applSeqNum, applId, TEMPLATE_SECURITY_STATUS, feedSource
        );
    }

    // ==================== NEWS (B) ====================

    private MarketDataEvent mapNews(Message msg, String feedSource) {
        String applId = getStringField(msg, "ApplID");
        long applSeqNum = getLongField(msg, "ApplSeqNum");

        MarketDataEntry entry = new MarketDataEntry();
        entry.setApplId(applId);
        entry.setApplSeqNum(applSeqNum);
        entry.setMsgType("B");
        entry.setTemplateId(TEMPLATE_NEWS);

        log.info("News received: headline={}", getStringField(msg, "Headline"));
        return new MarketDataEvent(
                MarketDataEvent.EventType.NEWS,
                Collections.singletonList(entry),
                applSeqNum, applId, TEMPLATE_NEWS, feedSource
        );
    }

    // ==================== QUOTE REQUEST (R) ====================

    private MarketDataEvent mapQuoteRequest(Message msg, String feedSource) {
        String applId = getStringField(msg, "ApplID");
        long applSeqNum = getLongField(msg, "ApplSeqNum");

        MarketDataEntry entry = new MarketDataEntry();
        entry.setApplId(applId);
        entry.setApplSeqNum(applSeqNum);
        entry.setMsgType("R");
        entry.setTemplateId(TEMPLATE_QUOTE_REQUEST);

        return new MarketDataEvent(
                MarketDataEvent.EventType.INCREMENTAL_REFRESH,
                Collections.singletonList(entry),
                applSeqNum, applId, TEMPLATE_QUOTE_REQUEST, feedSource
        );
    }

    // ==================== REQUEST REJECT (Y) ====================

    private MarketDataEvent mapRequestReject(Message msg, String feedSource) {
        String mdReqId = getStringField(msg, "MDReqID");
        String reason = getStringField(msg, "MDReqRejReason");
        String text = getStringField(msg, "Text");
        log.warn("MarketDataRequestReject: reqId={}, reason={}, text={}", mdReqId, reason, text);
        return null;
    }

    // ==================== FIELD EXTRACTION HELPERS ====================

    /**
     * Parse the integer template ID from a MessageTemplate.
     * OpenFAST 1.1.1 returns String from getId().
     */
    private int parseTemplateId(MessageTemplate template) {
        try {
            return Integer.parseInt(template.getId());
        } catch (NumberFormatException e) {
            log.warn("Non-numeric template id: {}, using name-based lookup", template.getId());
            return -1;
        }
    }

    private String getStringField(GroupValue msg, String name) {
        try {
            if (msg.isDefined(name)) {
                return msg.getString(name);
            }
        } catch (Exception e) {
            log.trace("Field {} not found or not string", name);
        }
        return null;
    }

    private int getIntField(GroupValue msg, String name, int defaultValue) {
        try {
            if (msg.isDefined(name)) {
                return msg.getInt(name);
            }
        } catch (Exception e) {
            log.trace("Field {} not found or not int", name);
        }
        return defaultValue;
    }

    private Integer getOptionalInt(GroupValue msg, String name) {
        try {
            if (msg.isDefined(name)) {
                return msg.getInt(name);
            }
        } catch (Exception e) {
            log.trace("Field {} not found or not int", name);
        }
        return null;
    }

    private long getLongField(GroupValue msg, String name) {
        try {
            if (msg.isDefined(name)) {
                return msg.getLong(name);
            }
        } catch (Exception e) {
            log.trace("Field {} not found or not long", name);
        }
        return 0;
    }

    private Long getOptionalLong(GroupValue msg, String name) {
        try {
            if (msg.isDefined(name)) {
                return msg.getLong(name);
            }
        } catch (Exception e) {
            log.trace("Field {} not found", name);
        }
        return null;
    }

    private BigDecimal getDecimalField(GroupValue msg, String name) {
        try {
            if (msg.isDefined(name)) {
                return msg.getBigDecimal(name);
            }
        } catch (Exception e) {
            log.trace("Field {} not found or not decimal", name);
        }
        return null;
    }

    private SequenceValue getSequenceField(GroupValue msg, String name) {
        try {
            if (msg.isDefined(name)) {
                return msg.getSequence(name);
            }
        } catch (Exception e) {
            log.trace("Sequence {} not found", name);
        }
        return null;
    }

    /**
     * Get the template registry (for testing/inspection).
     */
    public TemplateRegistry getTemplateRegistry() {
        return templateRegistry;
    }

    /**
     * Get the decoder context (for resetting state, etc.).
     */
    public Context getDecoderContext() {
        return decoderContext;
    }

    /**
     * Reset decoder state (should be done on reconnect or daily reset).
     */
    public void resetState() {
        if (decoderContext != null) {
            decoderContext.reset();
            log.info("Reset FAST decoder state");
        }
    }
}
