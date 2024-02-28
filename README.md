# Secret Network getrandom contract example

An example of how to use the [getrandom](https://github.com/rust-random/getrandom) library in a contract on Secret
Network using a custom `getrandom` implementation from [getrandom-runtime-seeded](https://github.com/luca992/getrandom-runtime-seeded)
seeded with Secret Network's on-chain randomness [Secret-VRF](https://docs.scrt.network/secret-network-documentation/development/secret-contract-fundamentals/secret-vrf-on-chain-randomness) when available.


### Why
Many rust libraries rely on the `getrandom` crate to generate random numbers.
Which does not support on `wasm32-unknown-unknown`, which is the target for secret network contracts.

A common issue encountered when targeting `wasm32-unknown-unknown` is:
```
Compiling getrandom v0.2.12
error: the wasm*-unknown-unknown targets are not supported by default, you may need to enable the "js" feature.
```

This example shows how to use a custom implementation of `getrandom` to allowing any rust library relying on `getrandom`
to be used in a secret network contract.
