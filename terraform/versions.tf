terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # S3 backend for state management (optional - uncomment after creating bucket)
  # backend "s3" {
  #   bucket = "time2gather-terraform-state"
  #   key    = "prod/terraform.tfstate"
  #   region = "ap-northeast-2"
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "time2gather"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}
