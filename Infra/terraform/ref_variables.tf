variable "AWS_USERNAME" {
  description = "AWS Username"
  type        = string
  default     = ""
}

variable "AWS_PASSWD" {
  description = "AWS password"
  type        = string
  default     = ""
  sensitive   = true
}