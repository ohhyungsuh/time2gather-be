# EC2 Instance (Import existing)
# This uses the existing EC2 instance instead of creating a new one

# Data source to get existing instance info
data "aws_instance" "existing" {
  instance_id = var.existing_ec2_id
}

# Import existing EC2 - after import, Terraform manages it
resource "aws_instance" "app" {
  # These values will be populated from the imported instance
  ami                         = data.aws_instance.existing.ami
  instance_type               = var.instance_type
  key_name                    = var.key_name
  subnet_id                   = data.aws_instance.existing.subnet_id
  vpc_security_group_ids      = var.existing_security_group_id != "" ? [var.existing_security_group_id] : [aws_security_group.ec2[0].id]
  associate_public_ip_address = true

  tags = {
    Name = "${var.project_name}-app"
  }

  lifecycle {
    # Prevent recreation - we're importing an existing instance
    ignore_changes = [
      ami,
      user_data,
      user_data_base64,
      root_block_device,
    ]
  }
}

# Elastic IP (Import existing)
resource "aws_eip" "app" {
  domain = "vpc"

  tags = {
    Name = "${var.project_name}-eip"
  }

  lifecycle {
    # Prevent recreation
    ignore_changes = [instance]
  }
}

# Associate EIP with EC2
resource "aws_eip_association" "app" {
  instance_id   = aws_instance.app.id
  allocation_id = aws_eip.app.id
}
