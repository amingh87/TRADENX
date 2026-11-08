//! JNI entry points for `com.tradenex.app.core.NativeCore`. Compiled with the `android` feature.

use crate::{candles, globe, minutes_to_change, overlap_minutes, session_mask, stats};
use jni::objects::{JByteArray, JDoubleArray, JFloatArray, JLongArray, JObject};
use jni::sys::{jboolean, jdouble, jfloat, jint};
use jni::JNIEnv;

#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nSessionMask<'l>(
    _env: JNIEnv<'l>,
    _this: JObject<'l>,
    minute: jint,
) -> jint {
    session_mask(minute)
}

#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nOverlapMinutes<'l>(
    _env: JNIEnv<'l>,
    _this: JObject<'l>,
    a: jint,
    b: jint,
) -> jint {
    overlap_minutes(a.max(0) as usize, b.max(0) as usize)
}

#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nMinutesToChange<'l>(
    _env: JNIEnv<'l>,
    _this: JObject<'l>,
    minute: jint,
    session: jint,
) -> jint {
    minutes_to_change(minute, session.max(0) as usize)
}

#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nStats<'l>(
    mut env: JNIEnv<'l>,
    _this: JObject<'l>,
    pnls: JDoubleArray<'l>,
) -> JDoubleArray<'l> {
    let len = env.get_array_length(&pnls).unwrap_or(0) as usize;
    let mut buf = vec![0f64; len];
    let _ = env.get_double_array_region(&pnls, 0, &mut buf);
    let s = stats(&buf);
    match env.new_double_array(4) {
        Ok(out) => {
            let _ = env.set_double_array_region(&out, 0, &s);
            out
        }
        Err(_) => unsafe { JDoubleArray::from_raw(std::ptr::null_mut()) },
    }
}

/// Returns the number of dots loaded, or -1 when the asset is invalid.
#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nGlobeLoad<'l>(
    env: JNIEnv<'l>,
    _this: JObject<'l>,
    bytes: JByteArray<'l>,
) -> jint {
    match env.convert_byte_array(&bytes) {
        Ok(b) => globe::load(&b).map(|n| n as jint).unwrap_or(-1),
        Err(_) => -1,
    }
}

/// Fills `out` with [x, y, depth, day, tag, lit] per visible dot and returns how many dots were written.
#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nGlobeProject<'l>(
    mut env: JNIEnv<'l>,
    _this: JObject<'l>,
    lon: jfloat,
    lat: jfloat,
    sx: jfloat,
    sy: jfloat,
    sz: jfloat,
    night: jboolean,
    out: JFloatArray<'l>,
) -> jint {
    let len = env.get_array_length(&out).unwrap_or(0) as usize;
    if len < globe::STRIDE {
        return 0;
    }
    let mut buf = vec![0f32; len];
    let n = globe::project_loaded(lon, lat, [sx, sy, sz], night != 0, &mut buf);
    if n > 0 {
        let _ = env.set_float_array_region(&out, 0, &buf[..n * globe::STRIDE]);
    }
    n as jint
}

/// Returns [open, high, low, close] for each candle, flattened.
#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nCandles<'l>(
    mut env: JNIEnv<'l>,
    _this: JObject<'l>,
    pnls: JDoubleArray<'l>,
    buckets: JLongArray<'l>,
    start: jdouble,
    keep: jint,
) -> JDoubleArray<'l> {
    let n = env.get_array_length(&pnls).unwrap_or(0) as usize;
    let m = env.get_array_length(&buckets).unwrap_or(0) as usize;
    let mut p = vec![0f64; n];
    let mut b = vec![0i64; m];
    let _ = env.get_double_array_region(&pnls, 0, &mut p);
    let _ = env.get_long_array_region(&buckets, 0, &mut b);
    let c = candles::candles(&p, &b, start, keep.max(0) as usize);
    let flat: Vec<f64> = c.iter().flatten().copied().collect();
    match env.new_double_array(flat.len() as i32) {
        Ok(out) => {
            let _ = env.set_double_array_region(&out, 0, &flat);
            out
        }
        Err(_) => unsafe { JDoubleArray::from_raw(std::ptr::null_mut()) },
    }
}
