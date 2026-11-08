//! Equity-curve candles. Same maths as `Analytics.candles` in Kotlin.

/// Trades must already be sorted by time. Consecutive trades with the same bucket key form one
/// candle `[open, high, low, close]`; the equity starts at `start`. Only the last `keep` candles are returned.
pub fn candles(pnls: &[f64], buckets: &[i64], start: f64, keep: usize) -> Vec<[f64; 4]> {
    let n = pnls.len().min(buckets.len());
    let mut out: Vec<[f64; 4]> = Vec::new();
    let mut equity = start;
    let mut i = 0;
    while i < n {
        let key = buckets[i];
        let open = equity;
        let (mut hi, mut lo) = (equity, equity);
        while i < n && buckets[i] == key {
            equity += pnls[i];
            if equity > hi {
                hi = equity;
            }
            if equity < lo {
                lo = equity;
            }
            i += 1;
        }
        out.push([open, hi, lo, equity]);
    }
    let skip = out.len().saturating_sub(keep);
    out.split_off(skip)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn groups_consecutive_buckets() {
        let c = candles(&[100.0, -50.0, 25.0], &[1, 1, 2], 1000.0, 10);
        assert_eq!(c, vec![[1000.0, 1100.0, 1000.0, 1050.0], [1050.0, 1075.0, 1050.0, 1075.0]]);
    }

    #[test]
    fn keeps_only_the_last_candles() {
        let c = candles(&[1.0, 1.0, 1.0], &[1, 2, 3], 0.0, 2);
        assert_eq!(c.len(), 2);
        assert_eq!(c[0][0], 1.0);
        assert_eq!(c[1][3], 3.0);
    }

    #[test]
    fn empty_and_mismatched_inputs_are_safe() {
        assert!(candles(&[], &[], 5.0, 3).is_empty());
        assert_eq!(candles(&[1.0, 2.0], &[7], 0.0, 3).len(), 1);
        assert!(candles(&[1.0], &[1], 0.0, 0).is_empty());
    }

    #[test]
    fn drawdown_inside_a_bucket_sets_the_low() {
        let c = candles(&[-30.0, -20.0, 80.0], &[4, 4, 4], 100.0, 1);
        assert_eq!(c, vec![[100.0, 130.0, 50.0, 130.0]]);
    }
}
