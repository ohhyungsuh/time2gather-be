variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "prod"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "time2gather"
}

# Domain
variable "domain_name" {
  description = "Root domain name"
  type        = string
  default     = "time2gather.org"
}

# EC2
variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.small"
}

variable "key_name" {
  description = "EC2 key pair name"
  type        = string
}

# Ports
variable "frontend_port" {
  description = "Frontend application port"
  type        = number
  default     = 3000
}

variable "backend_port" {
  description = "Backend application port"
  type        = number
  default     = 8080
}

# Existing resources (for import)
variable "existing_vpc_id" {
  description = "Existing VPC ID (leave empty to create new)"
  type        = string
  default     = ""
}

variable "existing_subnet_ids" {
  description = "Existing public subnet IDs (required if using existing VPC)"
  type        = list(string)
  default     = []
}

variable "existing_certificate_arn" {
  description = "Existing ACM certificate ARN"
  type        = string
}

# Existing EC2 resources (for import)
variable "existing_ec2_id" {
  description = "Existing EC2 instance ID to import"
  type        = string
  default     = ""
}

variable "existing_eip_allocation_id" {
  description = "Existing Elastic IP allocation ID"
  type        = string
  default     = ""
}

variable "existing_security_group_id" {
  description = "Existing EC2 security group ID"
  type        = string
  default     = ""
}
