@file:Suppress("VARIABLE_IN_SINGLETON_WITHOUT_THREAD_LOCAL")

package io.eqoty.dapp.secret

import co.touchlab.kermit.Logger
import io.eqoty.cosmwasm.std.types.Coin
import io.eqoty.dapp.secret.utils.BalanceUtils
import io.eqoty.dapp.secret.utils.NodeInfo
import io.eqoty.dapp.secret.utils.getNode
import io.eqoty.secretk.client.SigningCosmWasmClient
import io.eqoty.secretk.types.MsgSend
import io.eqoty.secretk.types.TxOptions
import io.eqoty.secretk.wallet.DirectSigningWallet

/***
 * IntegrationTests will be re-instantiated for each test.
 * So this Global object holds properties that do not need to
 * be recreated each test.
 */
object TestGlobals {
    lateinit var adminAddress: String

    val nodeInfo: NodeInfo = getNode("src/commonTest/resources/config/nodes.json")
    var clientBacking: SigningCosmWasmClient? = null
    val client: SigningCosmWasmClient get() = clientBacking!!
    val clientInitialized get() = clientBacking != null

    // Sets a client with which we can interact with secret network
    suspend fun ensureGlobalTestClientInitialized(endpoint: String, chainId: String, numberOfWalletAccounts: Int) {
        if (!clientInitialized) {
            val wallet = DirectSigningWallet() // Use default constructor of wallet to generate random mnemonic.
            val client = SigningCosmWasmClient.init(
                endpoint,
                wallet,
                chainId = chainId
            )
            clientBacking = client
            adminAddress = client.wallet!!.getAccounts()[0].address
            BalanceUtils.fillUpFromFaucet(nodeInfo, client, 100_000_000, adminAddress)
            intializeAccountBeforeExecuteWorkaround(adminAddress)
        }
        val existingWalletAccountsCount = (client.wallet as DirectSigningWallet).accounts.size
        if (existingWalletAccountsCount >= numberOfWalletAccounts) {
            return
        }

        (existingWalletAccountsCount until numberOfWalletAccounts).forEach { _ ->
            addAccountToClient()
        }
        (client.wallet!! as DirectSigningWallet).let { wallet ->
            wallet.addressToAccountSigningData.values.forEach { a ->
                Logger.i("Added random account to wallet w/ mnemonic: ${a.mnemonic?.map { it.concatToString() }}")
            }
            Logger.i("Initialized client with wallet accounts: ${wallet.accounts.map { it.address }}")
        }
    }

    suspend fun intializeAccountBeforeExecuteWorkaround(senderAddress: String) {
        // workaround for weird issue where you need to execute a tx once (where it errors) before execute or
        // simulate can be called successfully on a brand-new account:
        // https://discord.com/channels/360051864110235648/603225118545674241/1030724640315805716
        val msgs = listOf(
            MsgSend(
                fromAddress = senderAddress,
                toAddress = senderAddress,
                amount = listOf(Coin(1, "usrct")),
            )
        )
        try {
            client.execute(
                msgs,
                txOptions = TxOptions(gasLimit = 100000)
            )
        } catch (_: Throwable) {
        }
    }

    private suspend fun addAccountToClient(): String {
        val customerAccount = (client.wallet as DirectSigningWallet).addAccount()
        BalanceUtils.fillUpFromFaucet(nodeInfo, client, 100_000_000, customerAccount.publicData.address)
        intializeAccountBeforeExecuteWorkaround(customerAccount.publicData.address)
        return customerAccount.publicData.address
    }

}
