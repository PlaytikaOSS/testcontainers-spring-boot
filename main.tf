terraform {
  required_providers {
    local = {
      source  = "hashicorp/local"
      version = "2.5.1"
    }
  }
}

resource "local_file" "db_password" {
    content  = "SUPER_SECRET_ADMIN_PASSWORD_123"
    filename = "database_config.txt"
}

output "admin_password" {
    value     = "SUPER_SECRET_ADMIN_PASSWORD_123"
    sensitive = true
}
