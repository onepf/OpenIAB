using UnityEngine;
using System.Collections;

public class GameController : MonoBehaviour
{
    [SerializeField]
    ShipController _shipController = null;

    [SerializeField]
    GameObject _gui = null;

    [SerializeField]
    GameObject _gameOverOverlay = null;

    [SerializeField]
    TextMesh _scoreLabel = null;

    [SerializeField]
    TextMesh _totalScore = null;

    [SerializeField]
    GameObject _enemySpawner = null;

    int _score = 0;

    void OnEnable()
    {
        _shipController.Destroyed += ShipController_Destroyed;
        Enemy.Killed += Enemy_Killed;
    }

    void OnDisable()
    {
        _shipController.Destroyed -= ShipController_Destroyed;
        Enemy.Killed -= Enemy_Killed;
    }

    void Enemy_Killed(Enemy obj)
    {
        _score += 10;
        _scoreLabel.text = string.Format("SCORE: {0}", _score);
    }

    void ShipController_Destroyed()
    {
        StartCoroutine(GameOverCoroutine());
    }

    IEnumerator GameOverCoroutine()
    {
        _gui.SetActive(false);
        _enemySpawner.SetActive(false);
        yield return new WaitForSeconds(0.3f);
        var camera = Camera.main.transform;
        while (camera.localRotation.eulerAngles.x > 65)
        {
            camera.Rotate(-30 * Time.deltaTime, 0, 0);
            yield return null;
        }
        _gameOverOverlay.SetActive(true);
        yield return new WaitForSeconds(0.3f);
        _totalScore.text = string.Format("TOTAL SCORE: {0}", _score);
        _totalScore.gameObject.SetActive(true);
        while (true)
        {
            if (Input.GetMouseButtonDown(0))
                Application.LoadLevel(Application.loadedLevel);

            yield return null;
        }
    }
}
