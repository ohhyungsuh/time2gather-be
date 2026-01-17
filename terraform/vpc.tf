# VPC Configuration
# If using existing VPC, use data source. Otherwise create new.

locals {
  use_existing_vpc = var.existing_vpc_id != ""
  vpc_id           = local.use_existing_vpc ? var.existing_vpc_id : aws_vpc.main[0].id
  public_subnet_ids = local.use_existing_vpc ? var.existing_subnet_ids : [
    aws_subnet.public_a[0].id,
    aws_subnet.public_c[0].id
  ]
}

# Data source for existing VPC (optional, for reference)
data "aws_vpc" "existing" {
  count = local.use_existing_vpc ? 1 : 0
  id    = var.existing_vpc_id
}

# New VPC (created only if existing_vpc_id is not provided)
resource "aws_vpc" "main" {
  count                = local.use_existing_vpc ? 0 : 1
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "${var.project_name}-vpc"
  }
}

resource "aws_internet_gateway" "main" {
  count  = local.use_existing_vpc ? 0 : 1
  vpc_id = aws_vpc.main[0].id

  tags = {
    Name = "${var.project_name}-igw"
  }
}

resource "aws_subnet" "public_a" {
  count                   = local.use_existing_vpc ? 0 : 1
  vpc_id                  = aws_vpc.main[0].id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.aws_region}a"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.project_name}-public-a"
  }
}

resource "aws_subnet" "public_c" {
  count                   = local.use_existing_vpc ? 0 : 1
  vpc_id                  = aws_vpc.main[0].id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "${var.aws_region}c"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.project_name}-public-c"
  }
}

resource "aws_route_table" "public" {
  count  = local.use_existing_vpc ? 0 : 1
  vpc_id = aws_vpc.main[0].id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main[0].id
  }

  tags = {
    Name = "${var.project_name}-public-rt"
  }
}

resource "aws_route_table_association" "public_a" {
  count          = local.use_existing_vpc ? 0 : 1
  subnet_id      = aws_subnet.public_a[0].id
  route_table_id = aws_route_table.public[0].id
}

resource "aws_route_table_association" "public_c" {
  count          = local.use_existing_vpc ? 0 : 1
  subnet_id      = aws_subnet.public_c[0].id
  route_table_id = aws_route_table.public[0].id
}
