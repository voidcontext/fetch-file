package vdx.fetchfile

final case class HttpClientException(
  private val message: String,
  private val cause: Exception = null
) extends Exception(message, cause)
