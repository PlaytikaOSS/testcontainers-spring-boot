acl {
  enabled = true
  default_policy = "deny"
  down_policy = "extend-cache"
  tokens = {
    master = "78b7ad52-1f0b-e100-7b02-000001122333" # master consul access_token, use as bearer token in HTTP requests
  }
}
