using UnityEngine;
using System.Collections;

public class EnemySpawner : MonoBehaviour
{
    [SerializeField]
    Transform _enemyPrefab = null;

    IEnumerator Start()
    {
        while (true)
        {
            var pos = Camera.main.ViewportToWorldPoint(new Vector3(Random.Range(0.1f, 0.9f), 1.1f, 100));
            Instantiate(_enemyPrefab, pos, Quaternion.identity);
            yield return new WaitForSeconds(1.0f);
        }
    }
}
