use cosmwasm_std::{
    entry_point, to_binary, Binary, Deps, DepsMut, Env, MessageInfo, Response, StdError, StdResult,
};
use getrandom_runtime_seeded::init_getrandom;

use crate::msg::{ExecuteMsg, InstantiateMsg, QueryMsg, RandomsResponse};

#[entry_point]
pub fn instantiate(
    _deps: DepsMut,
    _env: Env,
    _info: MessageInfo,
    _msg: InstantiateMsg,
) -> StdResult<Response> {
    Ok(Response::default())
}

#[entry_point]
pub fn execute(
    deps: DepsMut,
    env: Env,
    _info: MessageInfo,
    msg: ExecuteMsg,
) -> StdResult<Response> {
    deps.api.debug("execute started");
    // print the block random to debug logs
    deps.api.debug(&format!(
        "block random length: {:?}",
        env.block.random.clone().unwrap().0.len()
    ));
    deps.api.debug(&format!(
        "block random: {:?}",
        env.block.random.clone().unwrap()
    ));
    let rng_seed: [u8; 32] = env
        .block
        .random
        .unwrap()
        .0
        .try_into()
        .map_err(|_| StdError::generic_err("Invalid block random"))?;
    init_getrandom(rng_seed);
    deps.api.debug("execute set random block");
    let res = match msg {
        ExecuteMsg::GetRandoms { .. } => {
            let mut rand1 = [0u8; 32];
            let mut rand2 = [0u8; 32];
            getrandom::getrandom(&mut rand1).unwrap();
            getrandom::getrandom(&mut rand2).unwrap();
            deps.api.debug(format!("rand1: {:?}", rand1).as_str());
            deps.api.debug(format!("rand2: {:?}", rand2).as_str());
            Response::new().set_data(to_binary(&RandomsResponse(vec![
                rand1.into(),
                rand2.into(),
            ]))?)
        }
    };

    Ok(res)
}

#[entry_point]
pub fn query(deps: Deps, env: Env, msg: QueryMsg) -> StdResult<Binary> {
    deps.api.debug("query started");
    deps.api.debug(&format!(
        "block random length: {:?}",
        env.block.random.clone().unwrap().0.len()
    ));
    deps.api.debug(&format!(
        "block random: {:?}",
        env.block.random.clone().unwrap()
    ));
    // env.block.random is not supported for queries, it returns an array of a single zero byte
    // init_getrandom(&env.block.random.unwrap().0);

    deps.api.debug("query set random block");
    let res = match msg {
        QueryMsg::GetRandoms { entropy } => {
            let rng_seed: [u8; 32] = entropy
                .0
                .try_into()
                .map_err(|_| StdError::generic_err("Entropy must be 32 bytes"))?;
            init_getrandom(rng_seed);
            let mut rand1 = [0u8; 32];
            let mut rand2 = [0u8; 32];
            deps.api.debug("calling getrandom");
            getrandom::getrandom(&mut rand1).unwrap();
            getrandom::getrandom(&mut rand2).unwrap();
            deps.api.debug(format!("rand1: {:?}", rand1).as_str());
            deps.api.debug(format!("rand2: {:?}", rand2).as_str());
            to_binary(&RandomsResponse(vec![rand1.into(), rand2.into()]))?
        }
    };

    Ok(res)
}

#[cfg(test)]
mod tests {
    use cosmwasm_std::from_binary;
    use cosmwasm_std::testing::{mock_dependencies, mock_env, mock_info};

    use super::*;

    // this doesn't actually test randomness, since custom getrandom is not used on
    // architecture supporting getrandom natively... theoretically it should work on
    // when running the test targeting wasm32-unknown-unknown
    #[test]
    fn get_randoms() {
        let mut deps = mock_dependencies();
        let env = mock_env();
        let info = mock_info("creator", &[]);
        let msg = InstantiateMsg {};
        let res = instantiate(deps.as_mut(), env.clone(), info.clone(), msg).unwrap();

        let msg = ExecuteMsg::GetRandoms {};
        let res = execute(deps.as_mut(), env.clone(), info.clone(), msg).unwrap();
        // assert randoms are different
        let randoms: RandomsResponse = from_binary(&res.data.unwrap()).unwrap();
        assert_ne!(randoms.0[0], randoms.0[1]);
    }
}
