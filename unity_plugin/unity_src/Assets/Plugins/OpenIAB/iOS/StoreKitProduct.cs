namespace OnePF {
	public struct StoreKitProduct {
	    public string localizedTitle;
	    public string localizedDescription;
	    public string priceSymbol;
	    public string localPrice;
		public string identifier;
		
		public override string ToString() {
			return string.Format("identifier={0}, title={1}, description={2}, price={3} {4}",
				identifier, localizedTitle, localizedDescription, localPrice, priceSymbol);
		}
	}
}
