variable "prefix" { type = string }
variable "account_id" { type = string }
variable "enable_repositories" { type = bool }
variable "services" { type = set(string) }
resource "aws_s3_bucket" "reports" {
  bucket        = "${var.prefix}-${var.account_id}-reports"
  force_destroy = false
  tags          = { zone = "main" }
}
resource "aws_s3_bucket_public_access_block" "reports" {
  bucket                  = aws_s3_bucket.reports.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
resource "aws_s3_bucket_server_side_encryption_configuration" "reports" {
  bucket = aws_s3_bucket.reports.id
  rule {
    apply_server_side_encryption_by_default { sse_algorithm = "AES256" }
  }
}
resource "aws_s3_bucket_versioning" "reports" {
  bucket = aws_s3_bucket.reports.id
  versioning_configuration { status = "Enabled" }
}
resource "aws_s3_bucket_policy" "tls" {
  bucket = aws_s3_bucket.reports.id
  policy = jsonencode({ Version = "2012-10-17", Statement = [{ Effect = "Deny", Principal = "*", Action = "s3:*", Resource = [aws_s3_bucket.reports.arn, "${aws_s3_bucket.reports.arn}/*"], Condition = { Bool = { "aws:SecureTransport" = "false" } } }] })
}
resource "aws_ecr_repository" "service" {
  for_each             = var.enable_repositories ? var.services : toset([])
  name                 = "${var.prefix}/${each.key}"
  image_tag_mutability = "IMMUTABLE"
  image_scanning_configuration { scan_on_push = true }
  tags = { zone = "shared" }
}
output "report_bucket_arn" { value = aws_s3_bucket.reports.arn }
output "repositories" { value = { for k, v in aws_ecr_repository.service : k => v.repository_url } }
