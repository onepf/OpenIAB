using UnityEngine;
using UnityEditor;
using System.Collections;
using System.Collections.Generic;
using System.IO;

public class CleanProject : ScriptableWizard
{
    public Object[] dependencyRoots = new Object[1];

    private HashSet<string> _excludedPaths;

    private static readonly string[] SafeDirectories = {"Pro Standard Assets", "Standard Assets", "Scripts", "Editor", "Gizmos"};
    private static readonly string[] SafeExtensions = {".meta"};

    [MenuItem("Tools/Clean Project")]
    public static void LaunchCleanProject()
    {
        DisplayWizard("Clean Project", typeof (CleanProject), "Clean");
    }
	
	public void OnWizardUpdate ()
	{
		if (dependencyRoots.Length == 0 || dependencyRoots[0] == null)
		{
			helpString = "Please drag in a dependenct root! (demo_forest scene)";
			isValid = false;
			return;
		}
		
		if (Application.isPlaying)
		{
			helpString = "Exit Play mode first!";
			isValid = false;
			return;
		}
		
		helpString = "Clean button will delete all unused assets INCLUDING SVN HISTORY. Make a backup before proceeding.";
		isValid = true;
	}
	
    public void OnWizardCreate()
    {
        DisableExternalVersionControl();

        DeleteUnusedAssets();
    }
	
	private void DisableExternalVersionControl ()
	{
		if(EditorSettings.externalVersionControl == ExternalVersionControl.Generic)
        {
            // Force disable external version control
            EditorSettings.externalVersionControl = ExternalVersionControl.Disabled;

            // Refresh (will remove all .meta files)
            AssetDatabase.Refresh();
			
			Debug.Log("Disabled external version control.");
        }
	}
	
	private void DeleteUnusedAssets ()
	{
		// Build excluded paths set
        _excludedPaths = new HashSet<string>();

        foreach (Object root in dependencyRoots)
        {
            ExcludeObject(root);
        }

        int n = 0;
        Object[] dependencies = EditorUtility.CollectDependencies(dependencyRoots);
        foreach (Object dependency in dependencies)
        {
            EditorUtility.DisplayProgressBar("Getting Paths", ExcludeObject(dependency), n++ / (float)dependencies.Length);
        }

        // Delete unneeded files
        WalkDirectory(new DirectoryInfo(Application.dataPath));

        EditorUtility.ClearProgressBar();

        // Refresh
        AssetDatabase.Refresh();
	}

    private int WalkDirectory(DirectoryInfo dir)
    {
        // ignore safe directories
        if (dir.Parent != null && dir.Parent.Name == "Assets" && (SafeDirectories as IList).Contains(dir.Name))
			return 0;

        // ignore svn
        if (dir.Name == ".svn")
			return 1;

        // Walk through all files/folders in directory
        FileSystemInfo[] contents = dir.GetFileSystemInfos();

        int n = 0;
        //int nRemoved = 0; // for test code path, useless if test == false
        foreach (FileSystemInfo info in contents)
        {
            EditorUtility.DisplayProgressBar("Walking " + dir.FullName, info.FullName, n++ / (float)contents.Length);

            if (info.GetType() == typeof(DirectoryInfo))
            {
                // recursively walk through the subdirectory
                WalkDirectory((DirectoryInfo) info);
            }
            else
            {
                // ignore dependencies and safe extensions
                if (!_excludedPaths.Contains(info.FullName) && !(SafeExtensions as IList).Contains(info.Extension))
                {
                    try
                    {
                        File.Delete(info.FullName);
						Debug.Log("Removed file " + info.FullName);
                    }
                    catch (System.Exception e)
                    {
                        Debug.LogError(e.ToString());
                    }
                }
            }
        }

        if (dir.GetFileSystemInfos().Length == 0)
        {    
            try
            {
                Directory.Delete(dir.FullName);
				Debug.Log("Removed empty folder " + dir.FullName);
            }
            catch (System.Exception e)
            {
                Debug.LogError(e.ToString());
            }
            return 2; // return 2 for the meta file
        }

        return 0;
    }

    private string ExcludeObject(Object obj)
    {
        if (!AssetDatabase.Contains(obj))
            return "";

        string path = AssetDatabase.GetAssetPath(obj);
        if (path == "") return "";
        path = Path.GetFullPath(path);
        _excludedPaths.Add(path);

        return path;
    }
}
