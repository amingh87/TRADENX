//! Land-dot globe: parse the `world_dots.bin` asset once, then rotate, cull and
//! shade every dot per frame. This is the hot loop of the Gold globe (about 5k dots at 60 fps).
//!
//! Asset format (little endian), produced by `tools/gen_world.mjs`:
//! `"TXW1"`, `u32 count`, then `count` records of
//! `i16 lon*100, i16 lat*100, u8 tag, u8 lit`.
//! `tag`: 0 land, 1 Sydney region, 2 Tokyo, 3 London, 4 New York. `lit`: city-light strength 0..255.

use std::sync::RwLock;

pub const MAGIC: &[u8; 4] = b"TXW1";
/// Floats written per visible dot: x, y, depth, day, tag, lit.
pub const STRIDE: usize = 6;

#[derive(Default, Debug, Clone, PartialEq)]
pub struct Dots {
    pub x: Vec<f32>,
    pub y: Vec<f32>,
    pub z: Vec<f32>,
    pub tag: Vec<u8>,
    pub lit: Vec<u8>,
}

impl Dots {
    pub fn len(&self) -> usize {
        self.x.len()
    }
    pub fn is_empty(&self) -> bool {
        self.x.is_empty()
    }
}

/// Unit vector for a longitude/latitude in degrees (x east, y north, z towards lon 0).
pub fn unit(lon_deg: f32, lat_deg: f32) -> [f32; 3] {
    let lo = lon_deg.to_radians();
    let la = lat_deg.to_radians();
    [la.cos() * lo.sin(), la.sin(), la.cos() * lo.cos()]
}

pub fn parse(bytes: &[u8]) -> Result<Dots, &'static str> {
    if bytes.len() < 8 || &bytes[0..4] != MAGIC {
        return Err("bad magic");
    }
    let n = u32::from_le_bytes([bytes[4], bytes[5], bytes[6], bytes[7]]) as usize;
    if bytes.len() < 8 + n * 6 {
        return Err("truncated");
    }
    let mut d = Dots {
        x: Vec::with_capacity(n),
        y: Vec::with_capacity(n),
        z: Vec::with_capacity(n),
        tag: Vec::with_capacity(n),
        lit: Vec::with_capacity(n),
    };
    for i in 0..n {
        let o = 8 + i * 6;
        let lon = i16::from_le_bytes([bytes[o], bytes[o + 1]]) as f32 / 100.0;
        let lat = i16::from_le_bytes([bytes[o + 2], bytes[o + 3]]) as f32 / 100.0;
        let u = unit(lon, lat);
        d.x.push(u[0]);
        d.y.push(u[1]);
        d.z.push(u[2]);
        d.tag.push(bytes[o + 4]);
        d.lit.push(bytes[o + 5]);
    }
    Ok(d)
}

/// Rotate so (`lon_deg`, `lat_deg`) faces the viewer, drop the far hemisphere and shade by the sun.
/// `sun` is a unit vector in the same frame as [`unit`]. `night` = false lights everything.
/// Returns the number of dots written to `out` (each [`STRIDE`] floats).
pub fn project(d: &Dots, lon_deg: f32, lat_deg: f32, sun: [f32; 3], night: bool, out: &mut [f32]) -> usize {
    let (sl, cl) = lon_deg.to_radians().sin_cos();
    let (sp, cp) = lat_deg.to_radians().sin_cos();
    let cap = out.len() / STRIDE;
    let mut n = 0usize;
    for i in 0..d.len() {
        let (x, y, z) = (d.x[i], d.y[i], d.z[i]);
        let x1 = x * cl - z * sl;
        let z1 = x * sl + z * cl;
        let y2 = y * cp - z1 * sp;
        let z2 = y * sp + z1 * cp;
        if z2 <= 0.02 {
            continue;
        }
        if n >= cap {
            break;
        }
        let day = if night {
            (0.5 + (x * sun[0] + y * sun[1] + z * sun[2]) * 3.0).clamp(0.0, 1.0)
        } else {
            1.0
        };
        let o = n * STRIDE;
        out[o] = x1;
        out[o + 1] = y2;
        out[o + 2] = z2;
        out[o + 3] = day;
        out[o + 4] = d.tag[i] as f32;
        out[o + 5] = d.lit[i] as f32 / 255.0;
        n += 1;
    }
    n
}

static LOADED: RwLock<Option<Dots>> = RwLock::new(None);

/// Parse and keep the dots for [`project_loaded`]. Returns the dot count.
pub fn load(bytes: &[u8]) -> Result<usize, &'static str> {
    let d = parse(bytes)?;
    let n = d.len();
    *LOADED.write().map_err(|_| "poisoned")? = Some(d);
    Ok(n)
}

pub fn project_loaded(lon_deg: f32, lat_deg: f32, sun: [f32; 3], night: bool, out: &mut [f32]) -> usize {
    match LOADED.read() {
        Ok(g) => match g.as_ref() {
            Some(d) => project(d, lon_deg, lat_deg, sun, night, out),
            None => 0,
        },
        Err(_) => 0,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn blob(points: &[(i16, i16, u8, u8)]) -> Vec<u8> {
        let mut b = MAGIC.to_vec();
        b.extend_from_slice(&(points.len() as u32).to_le_bytes());
        for (lon, lat, tag, lit) in points {
            b.extend_from_slice(&lon.to_le_bytes());
            b.extend_from_slice(&lat.to_le_bytes());
            b.push(*tag);
            b.push(*lit);
        }
        b
    }

    #[test]
    fn parse_round_trip() {
        let d = parse(&blob(&[(0, 0, 0, 0), (4500, 3000, 3, 255)])).unwrap();
        assert_eq!(d.len(), 2);
        assert!((d.z[0] - 1.0).abs() < 1e-6);
        assert_eq!(d.tag[1], 3);
        assert_eq!(d.lit[1], 255);
        assert!((d.y[1] - 0.5).abs() < 1e-5);
    }

    #[test]
    fn rejects_garbage() {
        assert!(parse(b"nope").is_err());
        let mut b = blob(&[(0, 0, 0, 0)]);
        b.truncate(10);
        assert_eq!(parse(&b), Err("truncated"));
    }

    #[test]
    fn centre_dot_is_front_and_far_side_is_culled() {
        let d = parse(&blob(&[(0, 0, 0, 0), (18000, 0, 0, 0), (9000, 0, 0, 0)])).unwrap();
        let mut out = [0f32; 6 * 8];
        let n = project(&d, 0.0, 0.0, [0.0, 0.0, 1.0], false, &mut out);
        assert_eq!(n, 1, "only lon 0 faces the viewer; lon 90 sits on the limb");
        assert!(out[0].abs() < 1e-6 && out[1].abs() < 1e-6 && (out[2] - 1.0).abs() < 1e-6);
    }

    #[test]
    fn rotation_moves_dots_sideways() {
        let d = parse(&blob(&[(4500, 0, 0, 0)])).unwrap();
        let mut out = [0f32; 6];
        assert_eq!(project(&d, 0.0, 0.0, [0.0, 0.0, 1.0], false, &mut out), 1);
        assert!((out[0] - 45f32.to_radians().sin()).abs() < 1e-5);
        assert_eq!(project(&d, 45.0, 0.0, [0.0, 0.0, 1.0], false, &mut out), 1);
        assert!(out[0].abs() < 1e-5, "centred after rotating to its longitude");
    }

    #[test]
    fn tilt_centres_latitude() {
        let d = parse(&blob(&[(0, 3000, 0, 0)])).unwrap();
        let mut out = [0f32; 6];
        assert_eq!(project(&d, 0.0, 30.0, [0.0, 0.0, 1.0], false, &mut out), 1);
        assert!(out[1].abs() < 1e-5);
    }

    #[test]
    fn night_shading_follows_the_sun() {
        let d = parse(&blob(&[(0, 0, 0, 0)])).unwrap();
        let mut out = [0f32; 6];
        project(&d, 0.0, 0.0, [0.0, 0.0, 1.0], true, &mut out);
        assert_eq!(out[3], 1.0);
        project(&d, 0.0, 0.0, [0.0, 0.0, -1.0], true, &mut out);
        assert_eq!(out[3], 0.0);
        project(&d, 0.0, 0.0, [0.0, 0.0, -1.0], false, &mut out);
        assert_eq!(out[3], 1.0, "night shading off lights the whole globe");
    }

    #[test]
    fn output_buffer_limits_the_count() {
        let pts: Vec<_> = (0..20).map(|i| (i as i16, 0i16, 0u8, 0u8)).collect();
        let d = parse(&blob(&pts)).unwrap();
        let mut out = [0f32; 6 * 5];
        assert_eq!(project(&d, 0.0, 0.0, [0.0, 0.0, 1.0], false, &mut out), 5);
    }

    #[test]
    fn global_load_and_project() {
        assert_eq!(load(&blob(&[(0, 0, 2, 9)])).unwrap(), 1);
        let mut out = [0f32; 6];
        assert_eq!(project_loaded(0.0, 0.0, [0.0, 0.0, 1.0], false, &mut out), 1);
        assert_eq!(out[4], 2.0);
    }
}
