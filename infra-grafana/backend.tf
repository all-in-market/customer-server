terraform {
  backend "s3" {
    bucket         = "all-in-market-dev-state-323215070808"
    key            = "dev/grafana/terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "all-in-market-dev-lock"
    encrypt        = true
    kms_key_id     = "arn:aws:kms:ap-northeast-2:323215070808:key/282a36ab-423a-4d74-8242-dc3b76676783"
  }
}