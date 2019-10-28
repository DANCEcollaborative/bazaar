package edu.cmu.side.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.cmu.side.Workbench;
import edu.cmu.side.model.data.DocumentList;

public class RecipeManager{
	
	public enum Stage 
	{
		NONE("blank.side"),
		DOCUMENT_LIST("docs.side"), 
		FEATURE_TABLE("table.side"), 
		MODIFIED_TABLE("struct.side"),
		PREDICTION_ONLY("predict"), 
		TRAINED_MODEL("model.side"), 
		PREDICTION_RESULT("pred.side");
		
		public final String extension;
		
		private Stage(String ext)
		{
			this.extension = ext;
		}
	}

	static List<Recipe> recipes = new ArrayList<Recipe>();

	public Collection<Recipe> getRecipeCollectionByType(Stage... types){
		List<Recipe> filtered = new ArrayList<Recipe>();
		for(Recipe recipe : recipes){
			Stage stage = recipe.getStage();
			for(Stage type : types)
			{
				if (stage.equals(type))
				{
					filtered.add(recipe);
				}
			}
		}
		return filtered;
	}
	
	public boolean containsRecipe(Recipe rec){
		return recipes.contains(rec);
	}
	
	public void deleteRecipe(Recipe rec){
		recipes.remove(rec);
		Workbench.update(this);
		Workbench.update(rec.getStage());
	}
	
	public void addRecipe(Recipe rec){
		recipes.add(rec);
		Workbench.update(this);
		Workbench.update(rec.getStage());
	}
	
	public String getAvailableRecipeName(String base, Stage type){
		Set<String> unavailable = new TreeSet<String>();
		Collection<Recipe> recipes = getRecipeCollectionByType(type);
		for(Recipe r : recipes){
			unavailable.add(r.toString());
		}
		
		String key = base;
		if(key == null)
			key = "recipe";
		
		int counter = 1;
		while(unavailable.contains(key)){
			key = base + "_" + counter++;
		}
		return key;
	}
	
	
	public Recipe fetchDocumentListRecipe(DocumentList documents){
		Recipe recipe = Recipe.fetchRecipe();
		recipe.setDocumentList(documents);
		recipes.add(recipe);

		Workbench.update(this);
		Workbench.update(Stage.DOCUMENT_LIST);
		
		return recipe;
	}
	
	public RecipeManager(){

	}
}
