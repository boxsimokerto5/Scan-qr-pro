#!/usr/bin/env bash
# Script untuk membuat release keystore secara otomatis untuk Scan Qr Pro

KEYSTORE_NAME="my-upload-key.jks"
KEY_ALIAS="upload"
STORE_PASS="ScanQrPro2026SecurePass"
KEY_PASS="ScanQrPro2026SecurePass"

echo "========================================="
echo "Membuat Release Keystore Scan Qr Pro..."
echo "========================================="

if [ -f "$KEYSTORE_NAME" ]; then
    echo "Peringatan: File $KEYSTORE_NAME sudah ada."
    read -p "Apakah ingin menimpa (overwrite)? (y/N): " confirm
    if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
        echo "Dibatalkan."
        exit 0
    fi
    rm -f "$KEYSTORE_NAME"
fi

keytool -genkeypair -v \
  -keystore "$KEYSTORE_NAME" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "$STORE_PASS" \
  -keypass "$KEY_PASS" \
  -dname "CN=Scan Qr Pro, OU=Mobile, O=AI Studio, L=Jakarta, ST=Jakarta, C=ID"

echo ""
echo "✓ Keystore berhasil dibuat: $KEYSTORE_NAME"
echo "  Alias: $KEY_ALIAS"
echo "  Password: $STORE_PASS"
echo "========================================="
