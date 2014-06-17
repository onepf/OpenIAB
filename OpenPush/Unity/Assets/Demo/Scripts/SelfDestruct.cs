using UnityEngine;
using System.Collections;

public class SelfDestruct : MonoBehaviour
{
    [SerializeField]
    float _seconds = 1.0f;

    IEnumerator Start()
    {
        yield return new WaitForSeconds(_seconds);
        Destroy(gameObject);
    }
}
