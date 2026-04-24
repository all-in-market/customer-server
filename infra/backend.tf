terraform {
  backend "s3" {
    bucket         = "all-in-market-dev-terraform-state-323215070808"
    key            = "dev/terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "all-in-market-dev-terraform-lock"
    encrypt        = true
    kms_key_id     = "arn:aws:kms:ap-northeast-2:323215070808:key/09df1a33-cbaf-475a-874c-23d800cbe610"
  }
}