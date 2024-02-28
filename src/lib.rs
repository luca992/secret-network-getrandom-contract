use getrandom::register_custom_getrandom;
use getrandom_runtime_seeded::seeded_with_runtime_chacha_rng;

register_custom_getrandom!(seeded_with_runtime_chacha_rng);

pub mod contract;
pub mod msg;
