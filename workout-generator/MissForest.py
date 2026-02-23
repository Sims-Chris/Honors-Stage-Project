import pandas as pd
import numpy as np
from sklearn.experimental import enable_iterative_imputer
from sklearn.impute import IterativeImputer
from sklearn.ensemble import RandomForestRegressor
from sklearn.preprocessing import OrdinalEncoder

# Fill in missing data values

# 1. Load the data with 'latin1' encoding to handle special characters
# Update the path if your file is in a specific subfolder
file_path = 'workout-generator//AssessmentDataSept2022.csv' 
df = pd.read_csv(file_path, encoding='latin1')

# 2. Pre-processing: Convert numeric columns with string noise to numbers
# This handles entries that are meant to be numbers but might have text
if 'continuoushang' in df.columns:
    df['continuoushang'] = pd.to_numeric(df['continuoushang'], errors='coerce')

# 3. Separate Categorical and Numerical columns
cat_cols = df.select_dtypes(include=['object']).columns
num_cols = df.select_dtypes(exclude=['object']).columns

# 4. Encode Categorical data into numbers for the AI model
encoder = OrdinalEncoder(handle_unknown='use_encoded_value', unknown_value=np.nan)
df_encoded = df.copy()
df_encoded[cat_cols] = encoder.fit_transform(df[cat_cols])

# 5. Initialize MissForest (Iterative Imputer with Random Forest)
# We use a small number of estimators for speed
imputer = IterativeImputer(
    estimator=RandomForestRegressor(n_estimators=10, random_state=42),
    max_iter=10,
    random_state=42
)

# 6. Fill the gaps (Imputation)
print("Filling gaps... this may take a moment.")
df_imputed_array = imputer.fit_transform(df_encoded)
df_imputed = pd.DataFrame(df_imputed_array, columns=df_encoded.columns)

# 7. Convert encoded categories back to their original labels (strings)
for col in cat_cols:
    # Round to nearest integer to find the closest category index
    df_imputed[col] = df_imputed[col].round().astype(int)
    # Clip values to ensure they stay within the known category range
    num_cats = len(encoder.categories_[list(cat_cols).index(col)])
    df_imputed[col] = df_imputed[col].clip(0, num_cats - 1)

df_imputed[cat_cols] = encoder.inverse_transform(df_imputed[cat_cols])

# 8. Save the completed dataset
df_imputed.to_csv('Filled_AssessmentDataSept2022.csv', index=False)
print("Success! File saved as 'Filled_AssessmentDataSept2022.csv'")