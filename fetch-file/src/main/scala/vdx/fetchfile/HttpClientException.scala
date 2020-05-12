package vdx.fetchfile

final case class HttpClientException(
  private val message: String,
  private val cause: Exception = None.orNull
) extends Exception(message, cause)
