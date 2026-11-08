//! TRADENEX native core: session maths, trade statistics, equity candles and the globe projector.
//! Exposed to Kotlin through JNI (see `jni_api.rs` and app/.../core/NativeCore.kt).
//! Every function has a pure-Kotlin fallback, so the app still runs without this library.

pub mod candles;
pub mod globe;
#[cfg(feature = "android")]
mod jni_api;

/// Session table in UTC minutes of the day: (start, end).
/// Index: 0 Sydney, 1 Tokyo, 2 London, 3 New York.
const SESSIONS: [(i32, i32); 4] = [(1320, 420), (0, 540), (480, 1020), (780, 1320)];

fn is_open(minute: i32, start: i32, end: i32) -> bool {
    if start < end {
        minute >= start && minute < end
    } else {
        minute >= start || minute < end
    }
}

/// Bit i is set when session i is open.
pub fn session_mask(minute: i32) -> i32 {
    let m = minute.rem_euclid(1440);
    let mut mask = 0;
    for (i, (s, e)) in SESSIONS.iter().enumerate() {
        if is_open(m, *s, *e) {
            mask |= 1 << i;
        }
    }
    mask
}

/// Minutes during which both sessions are open in one day.
pub fn overlap_minutes(a: usize, b: usize) -> i32 {
    if a >= 4 || b >= 4 {
        return 0;
    }
    (0..1440)
        .filter(|m| {
            is_open(*m, SESSIONS[a].0, SESSIONS[a].1) && is_open(*m, SESSIONS[b].0, SESSIONS[b].1)
        })
        .count() as i32
}

/// Minutes until the given session flips between open and closed.
pub fn minutes_to_change(minute: i32, session: usize) -> i32 {
    if session >= 4 {
        return 0;
    }
    let (s, e) = SESSIONS[session];
    let now = minute.rem_euclid(1440);
    let open = is_open(now, s, e);
    for step in 1..=1440 {
        let m = (now + step) % 1440;
        if is_open(m, s, e) != open {
            return step;
        }
    }
    0
}

/// [net, win_rate (0..1), profit_factor, max_drawdown (<= 0)]
pub fn stats(pnls: &[f64]) -> [f64; 4] {
    if pnls.is_empty() {
        return [0.0, 0.0, 0.0, 0.0];
    }
    let mut net = 0.0;
    let mut gain = 0.0;
    let mut loss = 0.0;
    let mut wins = 0usize;
    let mut equity = 0.0;
    let mut peak = 0.0;
    let mut dd = 0.0;
    for p in pnls {
        net += p;
        if *p > 0.0 {
            gain += p;
            wins += 1;
        } else {
            loss += -p;
        }
        equity += p;
        if equity > peak {
            peak = equity;
        }
        if equity - peak < dd {
            dd = equity - peak;
        }
    }
    let pf = if loss > 0.0 {
        gain / loss
    } else if gain > 0.0 {
        99.0
    } else {
        0.0
    };
    [net, wins as f64 / pnls.len() as f64, pf, dd]
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn london_ny_overlap_is_four_hours() {
        assert_eq!(overlap_minutes(2, 3), 240);
    }

    #[test]
    fn sydney_wraps_midnight() {
        assert_eq!(session_mask(23 * 60) & 1, 1);
        assert_eq!(session_mask(8 * 60) & 1, 0);
    }

    #[test]
    fn stats_basic() {
        let s = stats(&[100.0, -50.0, 150.0, -100.0]);
        assert_eq!(s[0], 100.0);
        assert_eq!(s[1], 0.5);
        assert!((s[2] - 250.0 / 150.0).abs() < 1e-9);
        assert_eq!(s[3], -100.0);
    }
}
