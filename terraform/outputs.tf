output "alb_dns_name" {
  description = "ALB DNS name"
  value       = aws_lb.main.dns_name
}

output "ec2_public_ip" {
  description = "EC2 public IP"
  value       = aws_eip.app.public_ip
}

output "ec2_instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.app.id
}

output "frontend_url" {
  description = "Frontend URL"
  value       = "https://${var.domain_name}"
}

output "backend_url" {
  description = "Backend URL"
  value       = "https://api.${var.domain_name}"
}

output "vpc_id" {
  description = "VPC ID"
  value       = local.vpc_id
}

output "estimated_monthly_cost" {
  description = "Estimated monthly cost breakdown"
  value = <<-EOF
    Estimated Monthly Cost:
    - ALB: ~$16 (1 ALB) -> Now: ~$8 (was 2 ALBs)
    - EC2 (t3.small): ~$15
    - Route53: ~$0.50/zone + queries
    - Data Transfer: varies
    
    Total Estimate: ~$25-30/month
    Savings: ~$15-20/month from ALB consolidation
  EOF
}
