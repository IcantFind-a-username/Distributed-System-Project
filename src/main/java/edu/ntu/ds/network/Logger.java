package edu.ntu.ds.network;

import edu.ntu.ds.protocol.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Structured logging for server and client operations.
 * 
 * Provides clear, formatted output for debugging and demonstration purposes.
 */
public class Logger {
    
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    private final String prefix;
    private boolean verbose;
    
    public Logger(String prefix) {
        this.prefix = prefix;
        this.verbose = true;
    }
    
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    /**
     * Log an informational message
     */
    public void info(String message) {
        log("INFO", message);
    }
    
    /**
     * Log a warning message
     */
    public void warn(String message) {
        log("WARN", message);
    }
    
    /**
     * Log an error message
     */
    public void error(String message) {
        log("ERROR", message);
    }
    
    /**
     * Log an error with exception
     */
    public void error(String message, Throwable t) {
        log("ERROR", message + " - " + t.getMessage());
        if (verbose) {
            t.printStackTrace();
        }
    }
    
    /**
     * Log a debug message (only if verbose mode is enabled)
     */
    public void debug(String message) {
        if (verbose) {
            log("DEBUG", message);
        }
    }
    
    /**
     * Log incoming request details
     */
    public void logRequest(Message msg, String source) {
        Header h = msg.getHeader();
        StringBuilder sb = new StringBuilder();
        sb.append("REQUEST from ").append(source);
        sb.append(" | op=").append(h.getOpCode());
        sb.append(" | clientId=").append(h.getClientId());
        sb.append(" | reqId=").append(h.getRequestId());
        sb.append(" | semantics=").append(h.getSemantics());
        
        if (verbose) {
            sb.append("\n         TLVs: ");
            for (TlvField field : msg.getPayload().getFields()) {
                sb.append(field.getType().getName()).append("=");
                switch (field.getType().getValueType()) {
                    case STRING:
                        // Don't log password values
                        if (field.getType() == TlvType.PASSWORD) {
                            sb.append("****");
                        } else {
                            sb.append("\"").append(field.getStringValue()).append("\"");
                        }
                        break;
                    case UINT8:
                        sb.append(field.getUint8Value() & 0xFF);
                        break;
                    case UINT32:
                        sb.append(field.getUint32Value());
                        break;
                    case INT64:
                        sb.append(field.getInt64Value());
                        break;
                }
                sb.append(", ");
            }
        }
        
        info(sb.toString());
    }
    
    /**
     * Log outgoing reply details
     */
    public void logReply(Message msg, boolean fromCache, String destination) {
        Header h = msg.getHeader();
        StringBuilder sb = new StringBuilder();
        sb.append("REPLY to ").append(destination);
        sb.append(" | status=").append(h.getStatus());
        sb.append(" | reqId=").append(h.getRequestId());
        if (fromCache) {
            sb.append(" | [FROM AMO CACHE]");
        }
        
        info(sb.toString());
    }
    
    /**
     * Log callback notification
     */
    public void logCallback(Message msg, String destination) {
        Header h = msg.getHeader();
        Payload p = msg.getPayload();
        StringBuilder sb = new StringBuilder();
        sb.append("CALLBACK to ").append(destination);
        sb.append(" | op=").append(h.getOpCode());
        
        String accountNo = p.getAccountNo();
        if (accountNo != null) {
            sb.append(" | account=").append(accountNo);
        }
        
        Long amount = p.getAmountCents();
        if (amount != null) {
            sb.append(" | balance=").append(formatCents(amount));
        }
        
        info(sb.toString());
    }
    
    /**
     * Log client send action
     */
    public void logSend(Message msg, String destination, int attempt) {
        Header h = msg.getHeader();
        StringBuilder sb = new StringBuilder();
        sb.append("SEND to ").append(destination);
        sb.append(" | op=").append(h.getOpCode());
        sb.append(" | reqId=").append(h.getRequestId());
        sb.append(" | attempt=").append(attempt);
        
        info(sb.toString());
    }
    
    /**
     * Log client receive action
     */
    public void logReceive(Message msg, String source) {
        Header h = msg.getHeader();
        StringBuilder sb = new StringBuilder();
        sb.append("RECV from ").append(source);
        sb.append(" | status=").append(h.getStatus());
        sb.append(" | reqId=").append(h.getRequestId());
        
        info(sb.toString());
    }
    
    /**
     * Log timeout event
     */
    public void logTimeout(int attempt, int maxAttempts, long timeoutMs) {
        warn(String.format("Timeout (attempt %d/%d, timeout=%dms)", attempt, maxAttempts, timeoutMs));
    }
    
    /**
     * Log packet loss simulation event
     */
    public void logSimulatedLoss(String type, long requestId) {
        warn(String.format("[SIMULATED LOSS] %s dropped | reqId=%d", type, requestId));
    }
    
    private void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(TIME_FMT);
        System.out.printf("[%s] [%s] [%s] %s%n", timestamp, prefix, level, message);
    }
    
    /**
     * Format cents as currency string (e.g., 12345 -> "$123.45")
     */
    public static String formatCents(long cents) {
        boolean negative = cents < 0;
        long abs = Math.abs(cents);
        return String.format("%s$%d.%02d", negative ? "-" : "", abs / 100, abs % 100);
    }
}
