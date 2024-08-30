package org.browser.automation.exception;

import org.browser.automation.exception.base.LocalizedExceptionBase;

/**
 * Exception thrown when one or more essential fields are not set (i.e., null or empty).
 * This exception is designed to indicate that certain fields marked as essential
 * must be properly initialized before proceeding with an operation.
 */
public class EssentialFieldsNotSetException extends LocalizedExceptionBase {

    /**
     * Constructs a new {@code EssentialFieldsNotSetException} with a detailed message
     * indicating which essential fields are not set.
     *
     * @param fieldName the name of the essential field that is not set.
     */
    public EssentialFieldsNotSetException(String fieldName) {
        super("exception.essential.fields.not.set", fieldName);
    }

    /**
     * Constructs a new {@code EssentialFieldsNotSetException} with a custom message
     * and the specified field name that is not set.
     *
     * @param fieldName     the name of the essential field that is not set.
     * @param customMessage a custom message explaining the cause of the exception.
     */
    public EssentialFieldsNotSetException(String fieldName, String customMessage) {
        super(customMessage, "exception.essential.fields.not.set.custom", fieldName);
    }
}