package io.bdrc.ldspdi.exceptions;

public class RestException extends Exception {

    public static final int GENERIC_APP_ERROR_CODE = 5001;

    private static final long serialVersionUID = -5379981810772284216L;
    int status;
    int code;
    String link;
    String developerMessage;
    String message;

    public RestException() {
    }

    public RestException(int status, int code, String message, String developerMessage, String link) {
        super(message);
        this.status = status;
        this.code = code;
        this.message = message;
        this.developerMessage = developerMessage;
        this.link = link;
    }

    public RestException(int status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.message = message;
        this.developerMessage = null;
        this.link = null;
    }

    public RestException(int status, LdsError err) {
        super(err.getMsg());
        this.status = status;
        this.code = err.getCode();
        this.message = err.getMsg();
        this.developerMessage = null;
        this.link = null;
    }

    public int getStatus() {
        return status;
    }

    public int getCode() {
        return code;
    }

    public String getLink() {
        return link;
    }

    public String getDeveloperMessage() {
        return developerMessage;
    }

    public String getRestMessage() {
        return message;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setDeveloperMessage(String developerMessage) {
        this.developerMessage = developerMessage;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "RestException [status=" + status + ", code=" + code + ", link=" + link + ", developerMessage=" + developerMessage + ", message=" + message + "]";
    }

}