# Cloudinary Setup

Product image uploads use Cloudinary. Set these environment variables before running the product-service:

```bash
# Windows (PowerShell)
$env:CLOUDINARY_CLOUD_NAME="your_cloud_name"
$env:CLOUDINARY_API_KEY="your_api_key"
$env:CLOUDINARY_API_SECRET="your_api_secret"

# Linux / macOS
export CLOUDINARY_CLOUD_NAME=your_cloud_name
export CLOUDINARY_API_KEY=your_api_key
export CLOUDINARY_API_SECRET=your_api_secret
```

Get credentials from [Cloudinary Console](https://console.cloudinary.com/).
