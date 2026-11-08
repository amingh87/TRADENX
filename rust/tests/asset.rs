//! The shipped world_dots.bin must parse, cover every session region and project cleanly.
use tradenex_core::globe::{parse, project, STRIDE};

fn asset() -> Vec<u8> {
    std::fs::read(concat!(env!("CARGO_MANIFEST_DIR"), "/../app/src/main/assets/world_dots.bin")).expect("asset exists")
}

#[test]
fn asset_parses_and_has_all_regions() {
    let d = parse(&asset()).expect("valid asset");
    assert!(d.len() > 3000 && d.len() < 8000, "dot count {}", d.len());
    for tag in 1..=4u8 {
        assert!(d.tag.iter().filter(|t| **t == tag).count() >= 10, "region {tag} too small");
    }
    assert!(d.lit.iter().any(|l| *l > 0), "city lights present");
}

#[test]
fn front_hemisphere_is_roughly_half_and_inside_the_unit_disk() {
    let d = parse(&asset()).unwrap();
    let mut out = vec![0f32; STRIDE * d.len()];
    let n = project(&d, 20.0, 15.0, [0.0, 0.0, 1.0], true, &mut out);
    assert!(n > d.len() / 4 && n < d.len() * 3 / 4, "visible {n} of {}", d.len());
    for i in 0..n {
        let (x, y) = (out[i * STRIDE], out[i * STRIDE + 1]);
        assert!(x * x + y * y <= 1.0001);
    }
}
