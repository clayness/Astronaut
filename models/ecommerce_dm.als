module ecommerce_dm

open util/boolean
sig string{}
sig Customer {
Customer_customerID: one Int,
}
fact {
all o1,o2:Customer|o1.Customer_customerID = o2.Customer_customerID => o1=o2
}

sig Order {
Order_orderID: one Int,
}
fact {
all o1,o2:Order|o1.Order_orderID = o2.Order_orderID => o1=o2
}

sig CustomerOrderAssociation{
CustomerOrderAssociation_customerID: one Int,
CustomerOrderAssociation_orderID: one Int,
}
fact {
all o1,o2:CustomerOrderAssociation|o1.CustomerOrderAssociation_customerID=o2.CustomerOrderAssociation_customerID&&o1.CustomerOrderAssociation_orderID=o2.CustomerOrderAssociation_orderID => o1=o2
all o:CustomerOrderAssociation| one c:Customer| o.CustomerOrderAssociation_customerID = c.Customer_customerID
all o:CustomerOrderAssociation| one c:Order| o.CustomerOrderAssociation_orderID = c.Order_orderID
}

sig ShippingCart {
ShippingCart_shippingCartID: one Int,
}
fact {
all o1,o2:ShippingCart|o1.ShippingCart_shippingCartID = o2.ShippingCart_shippingCartID => o1=o2
}

sig CustomerShippingCartAssociation{
CustomerShippingCartAssociation_customerID: one Int,
CustomerShippingCartAssociation_shippingCartID: one Int,
}
fact {
all o1,o2:CustomerShippingCartAssociation|o1.CustomerShippingCartAssociation_customerID=o2.CustomerShippingCartAssociation_customerID&&o1.CustomerShippingCartAssociation_shippingCartID=o2.CustomerShippingCartAssociation_shippingCartID => o1=o2
all o:CustomerShippingCartAssociation| one c:Customer| o.CustomerShippingCartAssociation_customerID = c.Customer_customerID
all o:CustomerShippingCartAssociation| one c:ShippingCart| o.CustomerShippingCartAssociation_shippingCartID = c.ShippingCart_shippingCartID
}

sig Item {
Item_ItemID: one Int,
Item_quantity: one Int,
}
fact {
all o1,o2:Item|o1.Item_ItemID = o2.Item_ItemID => o1=o2
}

sig CartItem {
CartItem_ItemID: one Int,
CartItem_cartItemID: one Int,
}
fact {
all o1,o2:CartItem|o1.CartItem_ItemID = o2.CartItem_ItemID => o1=o2
all o:CartItem| one c:Item| o.CartItem_ItemID = c.Item_ItemID
}

sig ShippingCartItemAssociation{
ShippingCartItemAssociation_shippingCartID: one Int,
ShippingCartItemAssociation_ItemID: one Int,
}
fact {
all o1,o2:ShippingCartItemAssociation|o1.ShippingCartItemAssociation_shippingCartID=o2.ShippingCartItemAssociation_shippingCartID&&o1.ShippingCartItemAssociation_ItemID=o2.ShippingCartItemAssociation_ItemID => o1=o2
all o:ShippingCartItemAssociation| one c:ShippingCart| o.ShippingCartItemAssociation_shippingCartID = c.ShippingCart_shippingCartID
all o:ShippingCartItemAssociation| one c:Item| o.ShippingCartItemAssociation_ItemID = c.Item_ItemID
}

sig OrderItem {
OrderItem_ItemID: one Int,
OrderItem_orderItemID: one Int,
OrderItem_status: one Int,
}
fact {
all o1,o2:OrderItem|o1.OrderItem_ItemID = o2.OrderItem_ItemID => o1=o2
all o:OrderItem| one c:Item| o.OrderItem_ItemID = c.Item_ItemID
}

sig OrderItemAssociation{
OrderItemAssociation_orderID: one Int,
OrderItemAssociation_ItemID: one Int,
}
fact {
all o1,o2:OrderItemAssociation|o1.OrderItemAssociation_orderID=o2.OrderItemAssociation_orderID&&o1.OrderItemAssociation_ItemID=o2.OrderItemAssociation_ItemID => o1=o2
all o:OrderItemAssociation| one c:Order| o.OrderItemAssociation_orderID = c.Order_orderID
all o:OrderItemAssociation| one c:Item| o.OrderItemAssociation_ItemID = c.Item_ItemID
}

sig Category {
Category_categoryID: one Int,
Category_categoryName: one string,
}
fact {
all o1,o2:Category|o1.Category_categoryID = o2.Category_categoryID => o1=o2
}

sig Product {
Product_productID: one Int,
Product_productName: one string,
Product_description: one string,
Product_price: one Int,
}
fact {
all o1,o2:Product|o1.Product_productID = o2.Product_productID => o1=o2
}

sig ProductCategoryAssociation{
ProductCategoryAssociation_productID: one Int,
ProductCategoryAssociation_categoryID: one Int,
}
fact {
all o1,o2:ProductCategoryAssociation|o1.ProductCategoryAssociation_productID=o2.ProductCategoryAssociation_productID&&o1.ProductCategoryAssociation_categoryID=o2.ProductCategoryAssociation_categoryID => o1=o2
all o:ProductCategoryAssociation| one c:Product| o.ProductCategoryAssociation_productID = c.Product_productID
all o:ProductCategoryAssociation| one c:Category| o.ProductCategoryAssociation_categoryID = c.Category_categoryID
}

sig Catalog {
Catalog_CatalogID: one Int,
}
fact {
all o1,o2:Catalog|o1.Catalog_CatalogID = o2.Catalog_CatalogID => o1=o2
}

sig ProductCatalogAssociation{
ProductCatalogAssociation_productID: one Int,
ProductCatalogAssociation_CatalogID: one Int,
}
fact {
all o1,o2:ProductCatalogAssociation|o1.ProductCatalogAssociation_productID=o2.ProductCatalogAssociation_productID&&o1.ProductCatalogAssociation_CatalogID=o2.ProductCatalogAssociation_CatalogID => o1=o2
all o:ProductCatalogAssociation| one c:Product| o.ProductCatalogAssociation_productID = c.Product_productID
all o:ProductCatalogAssociation| one c:Catalog| o.ProductCatalogAssociation_CatalogID = c.Catalog_CatalogID
}

sig ProductItemAssociation{
ProductItemAssociation_productID: one Int,
ProductItemAssociation_ItemID: one Int,
}
fact {
all o1,o2:ProductItemAssociation|o1.ProductItemAssociation_productID=o2.ProductItemAssociation_productID&&o1.ProductItemAssociation_ItemID=o2.ProductItemAssociation_ItemID => o1=o2
all o:ProductItemAssociation| one c:Product| o.ProductItemAssociation_productID = c.Product_productID
all o:ProductItemAssociation| one c:Item| o.ProductItemAssociation_ItemID = c.Item_ItemID
}

sig PhysicalProduct {
PhysicalProduct_productID: one Int,
PhysicalProduct_weight: one Int,
PhysicalProduct_availability: one Bool,
}
fact {
all o1,o2:PhysicalProduct|o1.PhysicalProduct_productID = o2.PhysicalProduct_productID => o1=o2
all o:PhysicalProduct| one c:Product| o.PhysicalProduct_productID = c.Product_productID
}

sig ElectronicProduct {
ElectronicProduct_productID: one Int,
ElectronicProduct_size: one string,
}
fact {
all o1,o2:ElectronicProduct|o1.ElectronicProduct_productID = o2.ElectronicProduct_productID => o1=o2
all o:ElectronicProduct| one c:Product| o.ElectronicProduct_productID = c.Product_productID
}

sig Service {
Service_productID: one Int,
Service_schedule: one string,
}
fact {
all o1,o2:Service|o1.Service_productID = o2.Service_productID => o1=o2
all o:Service| one c:Product| o.Service_productID = c.Product_productID
}

sig Asset {
Asset_assetID: one Int,
Asset_assetName: one string,
Asset_fileURI: one string,
}
fact {
all o1,o2:Asset|o1.Asset_assetID = o2.Asset_assetID => o1=o2
}

sig ProductAssetAssociation{
ProductAssetAssociation_productID: one Int,
ProductAssetAssociation_assetID: one Int,
}
fact {
all o1,o2:ProductAssetAssociation|o1.ProductAssetAssociation_productID=o2.ProductAssetAssociation_productID&&o1.ProductAssetAssociation_assetID=o2.ProductAssetAssociation_assetID => o1=o2
all o:ProductAssetAssociation| one c:Product| o.ProductAssetAssociation_productID = c.Product_productID
all o:ProductAssetAssociation| one c:Asset| o.ProductAssetAssociation_assetID = c.Asset_assetID
}

sig Media {
Media_assetID: one Int,
Media_mediaType: one Int,
}
fact {
all o1,o2:Media|o1.Media_assetID = o2.Media_assetID => o1=o2
all o:Media| one c:Asset| o.Media_assetID = c.Asset_assetID
}

sig Documents {
Documents_assetID: one Int,
Documents_excerpt: one string,
}
fact {
all o1,o2:Documents|o1.Documents_assetID = o2.Documents_assetID => o1=o2
all o:Documents| one c:Asset| o.Documents_assetID = c.Asset_assetID
}

pred show{
some Customer
some Order
some CustomerOrderAssociation
some ShippingCart
some CustomerShippingCartAssociation
some Item
some CartItem
some ShippingCartItemAssociation
some OrderItem
some OrderItemAssociation
some Category
some Product
some ProductCategoryAssociation
some Catalog
some ProductCatalogAssociation
some ProductItemAssociation
some PhysicalProduct
some ElectronicProduct
some Service
some Asset
some ProductAssetAssociation
some Media
some Documents
}
run show for 6 int
