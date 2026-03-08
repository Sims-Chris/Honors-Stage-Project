import pandas as pd
from sklearn.ensemble import RandomForestRegressor
import m2cgen as m2c
import warnings

warnings.filterwarnings('ignore')

print("1. Loading dataset...")
df = pd.read_csv("generator-2\\reducedAssesmentData.csv")

X = df[['bouldergrade_numeric', 'sex_encoded', 'height', 'weight']]
y = df.fillna(0)

# We map the inner Java class names to our CSV columns
metrics = [
    ("Pullups", "pullup reps"),
    ("WeightedPull", "weightedpull"),
    ("MaxHang", "maxhang"),
    ("ContinuousHang", "continuoushang"),
    ("Repeaters", "repeaters1"),
    ("PowL", "powl"),
    ("PowR", "powr"),
    ("Pushups", "pushup reps")
]

print("2. Training 8 AI Models and Converting to Java...")
java_code_combined = "public class BoulderingAI {\n\n"

for class_name, col_name in metrics:
    print(f"   -> Exporting AI for: {col_name}")
    # Train an individual model for each specific metric
    model = RandomForestRegressor(n_estimators=15, max_depth=6, random_state=42)
    model.fit(X, y[col_name])
    
    # Export just this model to Java
    code = m2c.export_to_java(model, class_name=class_name)
    
    # Modify it to be an inner static class so they all fit in one neat file
    code = code.replace(f"public class {class_name}", f"public static class {class_name}")
    java_code_combined += code + "\n\n"

java_code_combined += "}\n"

print("\n3. Saving to BoulderingAI.java...")
with open("BoulderingAI.java", "w") as f:
    f.write(java_code_combined)

print("Success! You can now drag the new BoulderingAI.java into Android Studio.")