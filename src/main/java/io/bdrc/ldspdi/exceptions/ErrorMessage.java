package io.bdrc.ldspdi.exceptions;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorMessage {

	/** contains the same HTTP Status code returned by the server */
	@JsonProperty("status")
	public int status;

	/** application specific error code */
	@JsonProperty("code")
	public int code;

	/** message describing the error */
	@JsonProperty("message")
	public String message;

	/** link point to page where the error message is documented */
	@JsonProperty("link")
	public String link;

	/** extra information that might useful for developers */
	@JsonProperty("developerMessage")
	public String developerMessage;

	public ErrorMessage(RestException ex) {
		this.link = ex.link;
		this.developerMessage = ex.developerMessage;
		this.code = ex.code;
		this.message = ex.message;
		this.status = ex.status;
	}

	public static ErrorMessage getErrorMessage(int code, LdsError ex) {
		RestException re = new RestException(code, ex);
		return new ErrorMessage(re);
	}

	public ErrorMessage() {
	}
}
