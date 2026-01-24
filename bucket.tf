resource "aws_s3_bucket" "leaked_data" {
  bucket = "company-private-vault"
  # This is a classic security failure
  acl    = "public-read" 
}
