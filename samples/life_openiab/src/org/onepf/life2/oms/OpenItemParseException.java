package org.onepf.life2.oms;

/**
 * User: Boris Minaev
 * Date: 21.04.13
 * Time: 21:52
 */
public class OpenItemParseException extends RuntimeException {
    public String response;

    OpenItemParseException(String response) {
        this.response = response;
    }
}
