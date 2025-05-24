package com.dam.dinamicdca

data class CryptoCurrency(
    val name: String,
    val ticker: String
) {
    companion object {
        val mainCryptos = listOf(
            CryptoCurrency("Bitcoin", "BTC-USD"),
            CryptoCurrency("Ethereum", "ETH-USD"),
            CryptoCurrency("Binance Coin", "BNB-USD"),
            CryptoCurrency("Cardano", "ADA-USD"),
            CryptoCurrency("Solana", "SOL-USD"),
            CryptoCurrency("XRP", "XRP-USD"),
            CryptoCurrency("Polkadot", "DOT-USD"),
            CryptoCurrency("Dogecoin", "DOGE-USD"),
            CryptoCurrency("Avalanche", "AVAX-USD"),
            CryptoCurrency("Polygon", "MATIC-USD"),
            CryptoCurrency("Chainlink", "LINK-USD"),
            CryptoCurrency("Uniswap", "UNI-USD"),
            CryptoCurrency("Litecoin", "LTC-USD"),
            CryptoCurrency("Bitcoin Cash", "BCH-USD"),
            CryptoCurrency("Stellar", "XLM-USD"),
            CryptoCurrency("VeChain", "VET-USD"),
            CryptoCurrency("Cosmos", "ATOM-USD"),
            CryptoCurrency("Tron", "TRX-USD"),
            CryptoCurrency("Monero", "XMR-USD"),
            CryptoCurrency("Algorand", "ALGO-USD")
        )
    }
} 