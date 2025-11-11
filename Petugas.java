import java.sql.*;
import javax.swing.*;

public class Petugas extends Thread { // Kelas Petugas mewarisi Thread agar bisa berjalan secara paralel (multi-threading)
    private String namaPetugas;
    private JTextArea logArea;  
    private JTable table;

    // Konstruktor: menerima nama petugas, area log, dan tabel
    public Petugas(String namaPetugas, JTextArea logArea, JTable table) {
        this.namaPetugas = namaPetugas;
        this.logArea = logArea;
        this.table = table;
    }

    @Override
    public void run() { // Metode utamadalam  thread, dan dijalankan ketika thread.start() dipanggil
        while (true) { // Loop berulang selama masih ada pasien yang menunggu
            synchronized (Petugas.class) { 
                // Menggunakan sinkronisasi agar dua petugas tidak mengambil pasien yang sama secara bersamaan
                try (Connection conn = dbConn.getConnection()) { // Koneksi ke database
                    conn.setAutoCommit(false);

                    // Query untuk mengambil pasien pertama yang statusnya 'Menunggu'
                    // FOR UPDATE untuk memastikan agar tidak diambil petugas lain
                    String ambil = "SELECT a.id_antrian, p.nama_pasien, p.keluhan " +
                                   "FROM antrian a JOIN pasien p ON a.id_pasien = p.id_pasien " +
                                   "WHERE a.status = 'Menunggu' ORDER BY a.id_antrian LIMIT 1 FOR UPDATE";

                    PreparedStatement ps = conn.prepareStatement(ambil);
                    ResultSet rs = ps.executeQuery();

                    // Jika ada pasien yang menunggu
                    if (rs.next()) {
                        int idAntrian = rs.getInt("id_antrian");
                        String namaPasien = rs.getString("nama_pasien");
                        String keluhan = rs.getString("keluhan");

                        // Ubah status pasien menjadi "Dilayani"
                        PreparedStatement psUpdate = conn.prepareStatement(
                                "UPDATE antrian SET status = 'Dilayani' WHERE id_antrian = ?");
                        psUpdate.setInt(1, idAntrian);
                        psUpdate.executeUpdate();
                        conn.commit(); // Commit transaksi agar perubahan tersimpan

                        // Tambahkan log ke area GUI
                        log("🩺 " + namaPetugas + " mulai melayani " + namaPasien + " (" + keluhan + ")");
                        updateTable(); // Refresh tampilan tabel di GUI

                        // Simulasikan waktu pelayanan pasien (acak antara 2-6 detik)
                        Thread.sleep((int) (Math.random() * 4000 + 2000));

                        // Setelah pelayanan selesai, ubah status menjadi "Selesai"
                        try (Connection conn2 = dbConn.getConnection()) {
                            PreparedStatement psDone = conn2.prepareStatement(
                                    "UPDATE antrian SET status = 'Selesai' WHERE id_antrian = ?");
                            psDone.setInt(1, idAntrian);
                            psDone.executeUpdate();
                        }

                        // Tambahkan log bahwa pelayanan selesai
                        log("✅ " + namaPetugas + " selesai melayani " + namaPasien);
                        updateTable(); // Refresh tabel kembali setelah perubahan status

                    } else {
                        // Jika tidak ada pasien yang menunggu
                        log("💤 " + namaPetugas + " tidak menemukan pasien menunggu. Selesai bertugas.");
                        break; // Keluar dari loop while dan hentikan thread
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
        });
    }

    private void updateTable() {
        SwingUtilities.invokeLater(() -> {
            ((MainFrame.TableModelAntrian) table.getModel()).refreshData();
        });
    }
}
