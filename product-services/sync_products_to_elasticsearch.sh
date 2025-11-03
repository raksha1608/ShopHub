#!/bin/bash

# Get all products from MongoDB via Product Service
products=$(curl -s http://localhost:9011/products)

# Index each product to Elasticsearch
echo "$products" | python3 << 'EOF'
import sys
import json
import urllib.request
import urllib.error

products = json.load(sys.stdin)

for p in products:
    product_id = p['id']
    
    # Prepare the product data for Elasticsearch
    es_product = {
        'productId': product_id,
        'name': p['name'],
        'category': p['category'],
        'brand': p['brand'],
        'description': p['description'],
        'imageUrl': p.get('imageUrl', ''),
        'price': p['merchants'][0]['price'] if p.get('merchants') and len(p['merchants']) > 0 else 0,
        'merchants': [
            {
                'merchantId': m['merchant_id'],
                'name': m.get('name', ''),
                'price': m['price'],
                'stock': m['stock'],
                'discount': 0
            }
            for m in p.get('merchants', [])
        ]
    }
    
    # Index to Elasticsearch
    url = f'http://localhost:9200/products_final/_doc/{product_id}'
    data = json.dumps(es_product).encode('utf-8')
    req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'}, method='PUT')
    
    try:
        with urllib.request.urlopen(req) as response:
            print(f'✅ Indexed: {p["name"]} (Status: {response.status})')
    except urllib.error.HTTPError as e:
        print(f'❌ Failed to index {p["name"]}: {e}')

print('\n✅ All products synced to Elasticsearch!')
EOF

