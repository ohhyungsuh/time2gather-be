# Security Groups

locals {
  use_existing_ec2_sg = var.existing_security_group_id != ""
  ec2_security_group_id = local.use_existing_ec2_sg ? var.existing_security_group_id : aws_security_group.ec2[0].id
}

# ALB Security Group (always create new for the consolidated ALB)
resource "aws_security_group" "alb" {
  name        = "${var.project_name}-alb-sg"
  description = "Security group for ALB"
  vpc_id      = local.vpc_id

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP (redirect to HTTPS)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-alb-sg"
  }
}

# EC2 Security Group (only create if not using existing)
resource "aws_security_group" "ec2" {
  count       = local.use_existing_ec2_sg ? 0 : 1
  name        = "${var.project_name}-ec2-sg"
  description = "Security group for EC2"
  vpc_id      = local.vpc_id

  ingress {
    description     = "Frontend from ALB"
    from_port       = var.frontend_port
    to_port         = var.frontend_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  ingress {
    description     = "Backend from ALB"
    from_port       = var.backend_port
    to_port         = var.backend_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-ec2-sg"
  }
}

# Add rules to existing EC2 security group to allow traffic from new ALB
resource "aws_security_group_rule" "existing_ec2_from_alb_fe" {
  count                    = local.use_existing_ec2_sg ? 1 : 0
  type                     = "ingress"
  from_port                = var.frontend_port
  to_port                  = var.frontend_port
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.alb.id
  security_group_id        = var.existing_security_group_id
  description              = "Frontend from new ALB"
}

resource "aws_security_group_rule" "existing_ec2_from_alb_be" {
  count                    = local.use_existing_ec2_sg ? 1 : 0
  type                     = "ingress"
  from_port                = var.backend_port
  to_port                  = var.backend_port
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.alb.id
  security_group_id        = var.existing_security_group_id
  description              = "Backend from new ALB"
}
