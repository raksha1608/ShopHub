#!/usr/bin/env node

/**
 * Script to sync products from Product Service to Elasticsearch via Search Service
 */

const PRODUCT_SERVICE_URL = 'http://localhost:9011';
const SEARCH_SERVICE_URL = 'http://localhost:8090';

async function fetchProducts() {
  console.log('📦 Fetching products from Product Service...');
  const response = await fetch(`${PRODUCT_SERVICE_URL}/products`);
  if (!response.ok) {
    throw new Error(`Failed to fetch products: ${response.status}`);
  }
  const products = await response.json();
  console.log(`✅ Found ${products.length} products`);
  return products;
}

async function indexProduct(product) {
  // Transform product to match Elasticsearch schema
  const esProduct = {
    productId: product.id,
    name: product.name,
    category: product.category,
    brand: product.brand,
    description: product.description,
    imageUrl: product.imageUrl || null,
    price: product.merchants && product.merchants.length > 0
      ? product.merchants[0].price
      : 0,
    merchants: product.merchants ? product.merchants.map(m => ({
      merchantId: m.merchant_id,
      name: m.name || "ShopX",
      price: m.price,
      stock: m.stock,
      discount: 0
    })) : []
  };

  const response = await fetch(`${SEARCH_SERVICE_URL}/products/add`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(esProduct)
  });

  if (!response.ok) {
    throw new Error(`Failed to index product ${product.name}: ${response.status}`);
  }

  return response.json();
}

async function syncProducts() {
  try {
    const products = await fetchProducts();
    
    console.log('\n🔄 Syncing products to Elasticsearch...\n');
    
    let successCount = 0;
    let errorCount = 0;

    for (const product of products) {
      try {
        await indexProduct(product);
        successCount++;
        console.log(`✅ [${successCount}/${products.length}] Indexed: ${product.name}`);
      } catch (error) {
        errorCount++;
        console.error(`❌ Failed to index ${product.name}:`, error.message);
      }
    }

    console.log('\n' + '='.repeat(60));
    console.log(`✅ Successfully indexed: ${successCount} products`);
    console.log(`❌ Failed: ${errorCount} products`);
    console.log('='.repeat(60));
    console.log('\n🎉 Sync complete! You can now search for products.');
    
  } catch (error) {
    console.error('❌ Error syncing products:', error);
    process.exit(1);
  }
}

// Run the sync
syncProducts();

