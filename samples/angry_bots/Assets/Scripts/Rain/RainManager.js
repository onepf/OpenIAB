
#pragma strict

class RainManager extends MonoBehaviour 
{
	public var minYPosition : float = 0.0;
	
	public var numberOfParticles : int = 400;
	public var areaSize : float= 40.0f;
	public var areaHeight : float = 15.0;
	public var fallingSpeed : float= 23.0;
	public var particleSize : float = 0.2f;
	public var flakeRandom : float = 0.1;
	
	public var preGennedMeshes : Mesh [];
	private var preGennedIndex : int = 0;		

	public var generateNewAssetsOnStart : boolean = false;	

	public function Start () {
		#if UNITY_EDITOR	
			if (generateNewAssetsOnStart) {
				// create & save 3 meshes
				var m1 : Mesh = CreateMesh ();		
				var m2 : Mesh = CreateMesh ();		
				var m3 : Mesh = CreateMesh ();
				AssetDatabase.CreateAsset(m1, "Assets/Objects/RainFx/" + gameObject.name + "_LQ0.asset");
				AssetDatabase.CreateAsset(m2, "Assets/Objects/RainFx/" + gameObject.name + "_LQ1.asset");
				AssetDatabase.CreateAsset(m3, "Assets/Objects/RainFx/" + gameObject.name + "_LQ2.asset");
				Debug.Log ("Created new rain meshes in Assets/Objects/RainFx/");
			}		
		#endif
	}
	
	public function GetPreGennedMesh () : Mesh {
		return preGennedMeshes[(preGennedIndex++) % preGennedMeshes.Length];
	}	
	
	function CreateMesh () : Mesh {
		var mesh = new Mesh ();
		
		var cameraRight : Vector3 = Camera.main.transform.right;
		var cameraUp : Vector3 = (Vector3.up);
		
		var particleNum : int = QualityManager.quality > Quality.Medium ? numberOfParticles : numberOfParticles / 2;

		var verts : Vector3[] = new Vector3[4 * particleNum];
		var uvs : Vector2[]  = new Vector2[4 * particleNum];
		var uvs2 : Vector2[] = new Vector2[4 * particleNum];
		var normals : Vector3[] = new Vector3[4 * particleNum];
		
		var tris : int[] = new int[2 * 3 * particleNum];
 
		var position : Vector3;
		for (var i : int = 0; i < particleNum; i++)
		{
			var i4 : int = i * 4;
			var i6 : int = i * 6;

			position.x = areaSize * (Random.value - 0.5f);
			position.y = areaHeight * Random.value;
			position.z = areaSize * (Random.value - 0.5f);
			
			var rand : float = Random.value;
			var widthWithRandom : float = particleSize * 0.215f;// + rand * flakeRandom;
			var heightWithRandom : float = particleSize + rand * flakeRandom;

			verts[i4 + 0] = position - cameraRight * widthWithRandom - cameraUp * heightWithRandom;
			verts[i4 + 1] = position + cameraRight * widthWithRandom - cameraUp * heightWithRandom;
			verts[i4 + 2] = position + cameraRight * widthWithRandom + cameraUp * heightWithRandom;
			verts[i4 + 3] = position - cameraRight * widthWithRandom + cameraUp * heightWithRandom;
			
			normals[i4 + 0] = -Camera.main.transform.forward;
			normals[i4 + 1] = -Camera.main.transform.forward;
			normals[i4 + 2] = -Camera.main.transform.forward;
			normals[i4 + 3] = -Camera.main.transform.forward;

			uvs[i4 + 0] = new Vector2(0.0f, 0.0f);
			uvs[i4 + 1] = new Vector2(1.0f, 0.0f);
			uvs[i4 + 2] = new Vector2(1.0f, 1.0f);
			uvs[i4 + 3] = new Vector2(0.0f, 1.0f);

			uvs2[i4 + 0] = new Vector2(Random.Range(-2,2)*4.0, Random.Range(-1,1)*1.0);
			uvs2[i4 + 1] = new Vector2(uvs2[i4 + 0].x, uvs2[i4 + 0].y);
			uvs2[i4 + 2] = new Vector2(uvs2[i4 + 0].x, uvs2[i4 + 0].y);
			uvs2[i4 + 3] = new Vector2(uvs2[i4 + 0].x, uvs2[i4 + 0].y);

			tris[i6 + 0] = i4 + 0;
			tris[i6 + 1] = i4 + 1;
			tris[i6 + 2] = i4 + 2;
			tris[i6 + 3] = i4 + 0;
			tris[i6 + 4] = i4 + 2;
			tris[i6 + 5] = i4 + 3;
		}

		mesh.vertices = verts;
		mesh.triangles = tris;
		mesh.normals = normals;
		mesh.uv = uvs;
		mesh.uv2 = uvs2;
		mesh.RecalculateBounds ();
		
		return mesh;
	}	
}