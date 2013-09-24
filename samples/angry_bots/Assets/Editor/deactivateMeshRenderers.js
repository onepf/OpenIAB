// MenuItem adds a menu item in the GameObject menu
// and executes the following function when clicked
@MenuItem ("GameObject/Deactivate Renderers")
static function deactivateRenderers ()
{
    // Get all selected game objects that we are allowed to modify!
    var gos = Selection.gameObjects;
    // Change the values of all
    for (var go in gos) 
    {
        go.renderer.enabled = false;
    }
}

@MenuItem ("GameObject/Activate Renderers")
static function activateRenderers ()
{
    // Get all selected game objects that we are allowed to modify!
    var gos = Selection.gameObjects;
    // Change the values of all
    for (var go in gos) 
    {
        go.renderer.enabled = true;
    }
}