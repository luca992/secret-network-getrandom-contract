package io.eqoty.dapp.secret

import DeployContractUtils
import co.touchlab.kermit.Logger
import io.eqoty.cosmwasm.std.types.CodeInfo
import io.eqoty.cosmwasm.std.types.ContractInfo
import io.eqoty.dapp.secret.TestGlobals.client
import io.eqoty.dapp.secret.utils.Constants
import io.eqoty.secretk.types.MsgExecuteContract
import io.eqoty.secretk.types.MsgInstantiateContract
import io.eqoty.secretk.types.TxOptions
import io.getenv
import io.ktor.util.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.Path
import okio.Path.Companion.toPath
import kotlin.math.ceil
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotSame

class IntegrationTests {

    private val contractCodePath: Path = getenv(Constants.CONTRACT_PATH_ENV_NAME)!!.toPath()

    suspend fun contractCodeInfo(senderAddress: String = TestGlobals.adminAddress): CodeInfo {
        return DeployContractUtils.getOrStoreCode(client, senderAddress, contractCodePath, null)
    }

    // Initialization procedure
    private suspend fun initializeAndUploadContract(): ContractInfo {
        val contractCodeInfo = contractCodeInfo()
        val instantiateMsgs = listOf(
            MsgInstantiateContract(
                admin = TestGlobals.adminAddress,
                sender = TestGlobals.adminAddress,
                codeId = contractCodeInfo.codeId.toInt(),
                initMsg = "{}",
                label = "Label: " + ceil(Random.nextDouble() * 10000),
                codeHash = null // will be set later
            )
        )
        return DeployContractUtils.instantiateCode(
            TestGlobals.client, contractCodeInfo, instantiateMsgs, 300_000
        ).let {
            ContractInfo(it.address, it.codeInfo.codeHash)
        }
    }

    private suspend fun queryGetRandoms(contractInfo: ContractInfo): List<String> {
        val contractMsg = """{"get_randoms": { "entropy": "${Random.nextBytes(32).encodeBase64()}"}}"""
        return Json.decodeFromString<List<String>>(
            client.queryContractSmart(
                contractInfo.address,
                contractMsg
            )
        )
    }

    private suspend fun executeGetRandoms(
        contractInfo: ContractInfo,
        senderAddress: String
    ): List<String> {
        val contractMsg = """{"get_randoms": {}}"""

        val msgs = listOf(
            MsgExecuteContract(
                sender = senderAddress,
                contractAddress = contractInfo.address,
                codeHash = contractInfo.codeHash,
                msg = contractMsg,
            )
        )
        val result = client.execute(
            msgs,
            txOptions = TxOptions(gasLimit = 100_000)
        )
        Logger.i("TX used ${result.gasUsed}")
        return Json.decodeFromString<List<String>>(result.data[0])
    }


    @BeforeTest
    fun beforeEach() = runTest {
        Logger.setTag("dapp")
    }

    @Test
    fun test_random() = runTest {
        with(TestGlobals.nodeInfo) {
            TestGlobals.ensureGlobalTestClientInitialized(grpcGatewayEndpoint, chainId, 0)
        }
        val contractInfo = initializeAndUploadContract()

        val executeGetRandoms0 = executeGetRandoms(contractInfo, TestGlobals.adminAddress)
        Logger.i("Execute Randoms Response: $executeGetRandoms0")
        val executeGetRandoms1 = executeGetRandoms(contractInfo, TestGlobals.adminAddress)
        Logger.i("Execute Randoms Response: $executeGetRandoms1")
        assertNotSame(executeGetRandoms0, executeGetRandoms1)
        val queryResponse0 = queryGetRandoms(contractInfo)
        Logger.i("Query Randoms Response: $queryResponse0")
        val queryResponse1 = queryGetRandoms(contractInfo)
        Logger.i("Query Randoms Response: $queryResponse1")
        assertNotSame(queryResponse0, queryResponse1)
    }


}
