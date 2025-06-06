using UnityEngine;

public class Items : MonoBehaviour
{
    [SerializeField] private string itemName;
    [SerializeField] private Sprite sprite;

    private UiController inventoryManager;

    void Start()
    {
        inventoryManager = GameObject.Find("U.I").GetComponent<UiController>();
    }

    private void OnTriggerEnter2D(Collider2D collision)
    {
        if (collision.gameObject.CompareTag("Player"))
        {
            Debug.Log("Player picked up: " + itemName);

            if (inventoryManager != null)
            {
                Debug.Log("Gửi item đến inventory: " + itemName + ", Sprite: " + (sprite != null ? sprite.name : "null"));

                // ✅ Chỉ xóa object nếu thêm thành công
                bool added = inventoryManager.AddItem(itemName, sprite);
                if (added)
                {
                    Destroy(gameObject);
                }
                else
                {
                    Debug.Log("Item trùng, không xóa khỏi map.");
                }
            }
            else
            {
                Debug.LogError("inventoryManager is NULL! Kiểm tra 'U.I' có UiController không.");
            }
        }
    }
}
