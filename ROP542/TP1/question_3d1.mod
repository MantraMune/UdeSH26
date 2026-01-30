
# Model to minimize the sum of absolute deviations from target values
var p;
var u1 >= 0;
var u2 >= 0;
var u3 >= 0;

# Objective function
minimize obj: u1 + u2 + u3;

# Constraints
subject to c1: 100 - p <= u1;
subject to c2: p - 100 <= u1;
subject to c3: 22 - p/4 <= u2;
subject to c4: p/4 - 22 <= u2;
subject to c5: 5 - p/25 <= u3;
subject to c6: p/25 - 5 <= u3;