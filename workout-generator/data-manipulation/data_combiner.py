import pandas as pd

# 1. Load both datasets
df_original = pd.read_csv('workout-generator//_Cleaned_AssessmentData.csv')
df_synthetic = pd.read_csv('workout-generator//_Cleaned_Synthetic_AssessmentData.csv')

# 2. Label the data so you know which is which
df_original['data_type'] = 'Original'
df_synthetic['data_type'] = 'Synthetic'

# 3. Combine them
# axis=0 stacks them on top of each other
df_combined = pd.concat([df_original, df_synthetic], axis=0, ignore_index=True)

# 4. Save the new "Super Dataset"
df_combined.to_csv('_Final_Data.csv', index=False)

print(f"Combined complete!")
print(f"Original rows: {len(df_original)}")
print(f"Synthetic rows: {len(df_synthetic)}")
print(f"Total dataset size: {len(df_combined)}")